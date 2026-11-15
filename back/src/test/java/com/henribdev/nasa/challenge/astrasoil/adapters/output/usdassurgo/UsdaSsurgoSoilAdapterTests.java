package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo.dtos.UsdaSsurgoQueryRequestDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo.dtos.UsdaSsurgoQueryResponseDto;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilComponent;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilTexture;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SurfaceLayer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import tools.jackson.databind.json.JsonMapper;

// The Feign client is mocked with rows copied from Soil Data Access's live answer for 42.0, -93.6 (Iowa)
@DisplayName("UsdaSsurgoSoilAdapter")
class UsdaSsurgoSoilAdapterTests {

    private static final List<String> COLUMNS = List.of(
        "mukey", "muname", "cokey", "compname", "comppct_r", "majcompflag", "drainagecl",
        "chkey", "om_r", "ph1to1h2o_r", "awc_r", "kwfact", "sandtotal_r", "silttotal_r", "claytotal_r",
        "texdesc", "rvindicator", "stratextsflag"
    );
    private static final String MAP_UNIT = "Hanlon-Spillville complex, channeled, 0 to 2 percent slopes";

    private UsdaSsurgoFeignClient usdaSsurgo;
    private UsdaSsurgoSoilAdapter adapter;

    @BeforeEach
    void setUp() {
        usdaSsurgo = mock(UsdaSsurgoFeignClient.class);
        adapter = new UsdaSsurgoSoilAdapter(usdaSsurgo, JsonMapper.builder().build());
    }

    // Arrays.asList, since some values are null
    private static List<String> row(String... values) {
        return Arrays.asList(values);
    }

    private static UsdaSsurgoQueryResponseDto iowa() {
        return new UsdaSsurgoQueryResponseDto(List.of(
            COLUMNS,
            row("411278", MAP_UNIT, "28635326", "Hanlon", "60", "Yes", "Moderately well drained",
                "85997371", "2.5", "6.5", "0.17", ".17", "65.4", "19.6", "15", "Fine sandy loam", "Yes", "No "),
            row("411278", MAP_UNIT, "28635327", "Spillville", "30", "Yes", "Moderately well drained",
                "85997373", "4.5", "6.5", "0.2", ".24", "41.1", "36.9", "22", "Loam", "Yes", "No "),
            row("411278", MAP_UNIT, "28635328", "Spillville", "5", "No ", "Moderately well drained",
                "85997374", "4.5", "6.5", "0.2", null, null, null, "22", "Loam", "Yes", "No "),
            row("411278", MAP_UNIT, "28635329", "Coland", "5", "No ", "Poorly drained",
                "85997375", "6", "6.7", "0.21", ".28", "22", "47", "31", "Clay loam", "Yes", "No "),
            row("411278", MAP_UNIT, "28635329", "Coland", "5", "No ", "Poorly drained",
                "85997375", "6", "6.7", "0.21", ".28", "22", "47", "31", "Silty clay loam", "No ", "No ")
        ));
    }

    @Test
    @DisplayName("Reads the map unit and its components, largest share first")
    void readsMapUnitAndComponents() {

        given(usdaSsurgo.query(any())).willReturn(iowa());

        SoilData soil = adapter.searchSoilData(42.0, -93.6).orElseThrow();

        assertThat(soil.mapUnitName()).isEqualTo(MAP_UNIT);
        assertThat(soil.components()).extracting(SoilComponent::name)
            .containsExactly("Hanlon", "Spillville", "Spillville", "Coland");
        assertThat(soil.components()).extracting(SoilComponent::sharePercent).containsExactly(60, 30, 5, 5);
        assertThat(soil.components()).extracting(SoilComponent::majorComponent).containsExactly(true, true, false, false);
        assertThat(soil.components().getLast().drainageClass()).isEqualTo("Poorly drained");
    }

    @Test
    @DisplayName("Reads the surface layer's values as numbers")
    void readsSurfaceLayer() {

        given(usdaSsurgo.query(any())).willReturn(iowa());

        SurfaceLayer hanlon = adapter.searchSoilData(42.0, -93.6).orElseThrow().components().getFirst().surfaceLayer();

        assertThat(hanlon).isEqualTo(new SurfaceLayer(2.5, 6.5, 0.17, 0.17, 65.4, 19.6, 15.0,
            List.of(new SoilTexture("Fine sandy loam", true, false))));
    }

    @Test
    @DisplayName("Keeps missing survey values as null")
    void keepsMissingValuesNull() {

        given(usdaSsurgo.query(any())).willReturn(iowa());

        SurfaceLayer minorSpillville = adapter.searchSoilData(42.0, -93.6).orElseThrow().components().get(2).surfaceLayer();

        assertThat(minorSpillville.erodibility()).isNull();
        assertThat(minorSpillville.sandPercent()).isNull();
        assertThat(minorSpillville.clayPercent()).isEqualTo(22.0);
    }

    @Test
    @DisplayName("Joins a layer's texture rows, representative first")
    void joinsTextureRows() {

        given(usdaSsurgo.query(any())).willReturn(iowa());

        SurfaceLayer coland = adapter.searchSoilData(42.0, -93.6).orElseThrow().components().getLast().surfaceLayer();

        assertThat(coland.textures()).containsExactly(
            new SoilTexture("Clay loam", true, false),
            new SoilTexture("Silty clay loam", false, false)
        );
    }

    @Test
    @DisplayName("Leaves the surface layer null when the survey has none")
    void noSurfaceLayer() {

        given(usdaSsurgo.query(any())).willReturn(new UsdaSsurgoQueryResponseDto(List.of(
            COLUMNS,
            row("1", "Rock outcrop", "2", "Rock outcrop", "100", "Yes", null,
                null, null, null, null, null, null, null, null, null, null, null)
        )));

        SoilComponent rock = adapter.searchSoilData(42.0, -93.6).orElseThrow().components().getFirst();

        assertThat(rock.surfaceLayer()).isNull();
    }

    @Test
    @DisplayName("Is empty where there's no soil survey (Soil Data Access answers {})")
    void emptyOutsideTheUs() {

        given(usdaSsurgo.query(any())).willReturn(new UsdaSsurgoQueryResponseDto(null));

        Optional<SoilData> soil = adapter.searchSoilData(-15.8, -47.9);

        assertThat(soil).isEmpty();
    }

    @Test
    @DisplayName("Writes the point with a dot as decimal separator, whatever the machine's language")
    void writesPointWithDot() {

        Locale original = Locale.getDefault();
        try {
            // Portuguese writes 42,0, which would break the query
            Locale.setDefault(Locale.forLanguageTag("pt-BR"));
            given(usdaSsurgo.query(any())).willReturn(new UsdaSsurgoQueryResponseDto(null));

            adapter.searchSoilData(42.0, -93.6);

            ArgumentCaptor<UsdaSsurgoQueryRequestDto> request = ArgumentCaptor.forClass(UsdaSsurgoQueryRequestDto.class);
            verify(usdaSsurgo).query(request.capture());
            assertThat(request.getValue().format()).isEqualTo("JSON+COLUMNNAME");
            assertThat(request.getValue().query())
                .contains("point(-93.6 42.0)")
                .doesNotContain("{longitude}", "{latitude}");
        }
        finally {
            Locale.setDefault(original);
        }
    }
}
