package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos.UsdaPlantAncestorDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos.UsdaPlantCharacteristicDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos.UsdaPlantProfileDto;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Crop;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.PlantCharacteristics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

// The Feign client is mocked with responses shaped like USDA's; the JSON mapper is real,
// so the name/value rows go through the same conversion as in production
@DisplayName("UsdaPlantsSearchAdapter")
class UsdaPlantsSearchAdapterTests {

    private static final Crop CORN = new Crop("Corn", "ZEMA", false);
    private static final Crop COTTON = new Crop("Cotton", "GOHI", false);

    private UsdaPlantsFeignClient usda;
    private UsdaPlantsSearchAdapter adapter;

    @BeforeEach
    void setUp() {
        usda = mock(UsdaPlantsFeignClient.class);
        adapter = new UsdaPlantsSearchAdapter(usda, JsonMapper.builder().build(), 4);
    }

    private static UsdaPlantProfileDto profile(int id, String symbol, String scientificName, String family) {
        return new UsdaPlantProfileDto(id, symbol, scientificName, List.of(
            new UsdaPlantAncestorDto("Kingdom", "<i>Plantae</i>"),
            new UsdaPlantAncestorDto("Family", family),
            new UsdaPlantAncestorDto("Genus", "<i>Genus</i> L.")
        ));
    }

    private static UsdaPlantCharacteristicDto species(String name, String value) {
        return new UsdaPlantCharacteristicDto(name, value, null);
    }

    @Test
    @DisplayName("Maps the profile and the species-level characteristics")
    void mapsProfileAndCharacteristics() {

        given(usda.getProfile("ZEMA")).willReturn(profile(26453, "ZEMA", "<i>Zea mays</i> L.", "<i>Poaceae</i> Barnhart"));
        given(usda.getCharacteristics(26453)).willReturn(List.of(
            species("Precipitation, Minimum", "20"),
            species("Precipitation, Maximum", "50"),
            species("Temperature, Minimum (°F)", "32"),
            species("Frost Free Days, Minimum", "90"),
            species("pH, Minimum", "5.5"),
            species("pH, Maximum", "7.5"),
            species("Adapted to Coarse Textured Soils", "No"),
            species("Adapted to Medium Textured Soils", "Yes"),
            species("Adapted to Fine Textured Soils", "No"),
            species("Drought Tolerance", "Low"),
            species("Anaerobic Tolerance", "None"),
            species("Active Growth Period", "Summer"),
            species("Nitrogen Fixation", "None"),
            species("Fertility Requirement", "High"),
            // A characteristic AstraSoil doesn't use is ignored
            species("Flower Color", "Yellow")
        ));

        List<Plant> plants = adapter.searchPlants(List.of(CORN));

        assertThat(plants).containsExactly(new Plant("Corn", "ZEMA", false, "Zea mays", "Poaceae",
            new PlantCharacteristics(20.0, 50.0, 32.0, 90, 5.5, 7.5, false, true, false,
                "Low", "None", "Summer", "None", "High")));
    }

    @Test
    @DisplayName("Ignores values that belong to a variety, not the species")
    void ignoresVarietyValues() {

        given(usda.getProfile("ZEMA")).willReturn(profile(26453, "ZEMA", "<i>Zea mays</i> L.", "<i>Poaceae</i>"));
        given(usda.getCharacteristics(26453)).willReturn(List.of(
            new UsdaPlantCharacteristicDto("Drought Tolerance", "High", "Some Variety"),
            species("Drought Tolerance", "Low")
        ));

        PlantCharacteristics characteristics = adapter.searchPlants(List.of(CORN)).getFirst().characteristics();

        assertThat(characteristics.droughtTolerance()).isEqualTo("Low");
    }

    @Test
    @DisplayName("Turns odd values into null instead of failing")
    void oddValuesBecomeNull() {

        given(usda.getProfile("ZEMA")).willReturn(profile(26453, "ZEMA", "<i>Zea mays</i> L.", "<i>Poaceae</i>"));
        given(usda.getCharacteristics(26453)).willReturn(List.of(
            species("Precipitation, Minimum", "N/A"),
            species("Frost Free Days, Minimum", ""),
            species("Drought Tolerance", "Low")
        ));

        PlantCharacteristics characteristics = adapter.searchPlants(List.of(CORN)).getFirst().characteristics();

        assertThat(characteristics.precipitationMinimumInches()).isNull();
        assertThat(characteristics.frostFreeDaysMinimum()).isNull();
        assertThat(characteristics.phMinimum()).isNull();
        assertThat(characteristics.adaptedToCoarseSoils()).isNull();
    }

    @Test
    @DisplayName("Leaves characteristics null when USDA has none (family-only crops)")
    void noCharacteristicsGivesNull() {

        given(usda.getProfile("GOHI")).willReturn(profile(65936, "GOHI", "<i>Gossypium hirsutum</i> L.", "<i>Malvaceae</i> Juss."));
        given(usda.getCharacteristics(65936)).willReturn(List.of());

        List<Plant> plants = adapter.searchPlants(List.of(COTTON));

        assertThat(plants).containsExactly(new Plant("Cotton", "GOHI", false, "Gossypium hirsutum", "Malvaceae", null));
    }

    @Test
    @DisplayName("Returns the plants in the order of the crops asked for")
    void keepsCropOrder() {

        given(usda.getProfile("ZEMA")).willReturn(profile(26453, "ZEMA", "<i>Zea mays</i> L.", "<i>Poaceae</i>"));
        given(usda.getProfile("GOHI")).willReturn(profile(65936, "GOHI", "<i>Gossypium hirsutum</i> L.", "<i>Malvaceae</i>"));
        given(usda.getCharacteristics(26453)).willReturn(List.of());
        given(usda.getCharacteristics(65936)).willReturn(List.of());

        List<Plant> plants = adapter.searchPlants(List.of(COTTON, CORN));

        assertThat(plants).extracting(Plant::usdaSymbol).containsExactly("GOHI", "ZEMA");
    }

    @Test
    @DisplayName("Fails when USDA returns a different plant than the one asked for")
    void wrongProfileFails() {

        // USDA answers 200 even for unknown symbols
        given(usda.getProfile("ZEMA")).willReturn(new UsdaPlantProfileDto(null, null, null, null));
        List<Crop> crops = List.of(CORN);

        assertThatThrownBy(() -> adapter.searchPlants(crops))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No USDA PLANTS profile for Corn (ZEMA)");
    }

    @Test
    @DisplayName("Works one crop at a time when configured with 1 parallel request")
    void worksWithOneParallelRequest() {

        UsdaPlantsSearchAdapter oneAtATime = new UsdaPlantsSearchAdapter(usda, JsonMapper.builder().build(), 1);
        given(usda.getProfile("ZEMA")).willReturn(profile(26453, "ZEMA", "<i>Zea mays</i> L.", "<i>Poaceae</i>"));
        given(usda.getProfile("GOHI")).willReturn(profile(65936, "GOHI", "<i>Gossypium hirsutum</i> L.", "<i>Malvaceae</i>"));
        given(usda.getCharacteristics(26453)).willReturn(List.of());
        given(usda.getCharacteristics(65936)).willReturn(List.of());

        List<Plant> plants = oneAtATime.searchPlants(List.of(COTTON, CORN));

        assertThat(plants).extracting(Plant::usdaSymbol).containsExactly("GOHI", "ZEMA");
    }

    @Test
    @DisplayName("Refuses fewer than 1 parallel request")
    void refusesInvalidParallelRequests() {

        JsonMapper jsonMapper = JsonMapper.builder().build();

        assertThatThrownBy(() -> new UsdaPlantsSearchAdapter(usda, jsonMapper, 0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
