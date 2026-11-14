package com.henribdev.nasa.challenge.astrasoil.adapters.output.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.UsdaPlantsSearchAdapter;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Crop;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.json.JsonMapper;

@DisplayName("FilePlantSearchCache")
class FilePlantSearchCacheTests {

    private static final Crop CORN = new Crop("Corn", "ZEMA", false);
    private static final Crop OATS = new Crop("Oats", "AVSA", true);

    private static final Plant CORN_PLANT = new Plant("Corn", "ZEMA", false, "Zea mays", "Poaceae", null);
    private static final Plant OATS_PLANT = new Plant("Oats", "AVSA", true, "Avena sativa", "Poaceae", null);

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    // Mocked so the tests never call USDA PLANTS
    private UsdaPlantsSearchAdapter usda;

    // A temporary folder, so the tests never touch the app's real plants copy
    @TempDir
    private Path folder;

    private Path file;

    @BeforeEach
    void setUp() {
        usda = mock(UsdaPlantsSearchAdapter.class);
        file = folder.resolve("plants.json");
    }

    private FilePlantSearchCache newCache() {
        return new FilePlantSearchCache(usda, jsonMapper, file.toString());
    }

    @Nested
    @DisplayName("When USDA PLANTS answers")
    class UsdaAnswers {

        @Test
        @DisplayName("Returns USDA's plants and saves them to the file")
        void searchReturnsUsdaPlantsAndSavesCopy() {

            given(usda.searchPlants(List.of(CORN))).willReturn(List.of(CORN_PLANT));

            List<Plant> plants = newCache().searchPlants(List.of(CORN));

            assertThat(plants).containsExactly(CORN_PLANT);
            assertThat(file).exists().content().contains("\"usdaSymbol\" : \"ZEMA\"");
        }

        @Test
        @DisplayName("Answers a repeated search from memory, without calling USDA again")
        void repeatedSearchUsesMemory() {

            given(usda.searchPlants(List.of(CORN))).willReturn(List.of(CORN_PLANT));
            FilePlantSearchCache cache = newCache();

            cache.searchPlants(List.of(CORN));
            List<Plant> second = cache.searchPlants(List.of(CORN));

            assertThat(second).containsExactly(CORN_PLANT);
            verify(usda, times(1)).searchPlants(anyList());
        }

        @Test
        @DisplayName("Calls USDA again when a crop isn't cached yet")
        void searchWithNewCropCallsUsda() {

            given(usda.searchPlants(List.of(CORN))).willReturn(List.of(CORN_PLANT));
            given(usda.searchPlants(List.of(CORN, OATS))).willReturn(List.of(CORN_PLANT, OATS_PLANT));
            FilePlantSearchCache cache = newCache();

            cache.searchPlants(List.of(CORN));
            List<Plant> plants = cache.searchPlants(List.of(CORN, OATS));

            assertThat(plants).containsExactly(CORN_PLANT, OATS_PLANT);
            verify(usda, times(2)).searchPlants(anyList());
        }

        @Test
        @DisplayName("Answers from memory with the crop's current name and cover-crop flag")
        void memoryAnswerUsesCurrentCropDetails() {

            given(usda.searchPlants(List.of(CORN))).willReturn(List.of(CORN_PLANT));
            FilePlantSearchCache cache = newCache();
            cache.searchPlants(List.of(CORN));

            Crop renamed = new Crop("Maize", "ZEMA", true);
            List<Plant> plants = cache.searchPlants(List.of(renamed));

            assertThat(plants).singleElement().satisfies(plant -> {
                assertThat(plant.name()).isEqualTo("Maize");
                assertThat(plant.coverCrop()).isTrue();
                assertThat(plant.scientificName()).isEqualTo("Zea mays");
            });
        }
    }

    @Nested
    @DisplayName("When USDA PLANTS fails")
    class UsdaFails {

        @Test
        @DisplayName("Uses the saved copy")
        void searchUsesSavedCopy() {

            // A first run saves the copy
            given(usda.searchPlants(List.of(CORN))).willReturn(List.of(CORN_PLANT));
            newCache().searchPlants(List.of(CORN));

            // A later run, with USDA down, starts with an empty memory
            UsdaPlantsSearchAdapter downUsda = mock(UsdaPlantsSearchAdapter.class);
            given(downUsda.searchPlants(anyList())).willThrow(new IllegalStateException("USDA down"));
            FilePlantSearchCache laterCache = new FilePlantSearchCache(downUsda, jsonMapper, file.toString());

            List<Plant> plants = laterCache.searchPlants(List.of(CORN));

            assertThat(plants).containsExactly(CORN_PLANT);
        }

        @Test
        @DisplayName("Uses the copy shipped with the app when none was saved yet")
        void searchUsesBundledCopy() {

            given(usda.searchPlants(anyList())).willThrow(new IllegalStateException("USDA down"));

            List<Plant> plants = newCache().searchPlants(List.of(CORN));

            assertThat(file).doesNotExist();
            assertThat(plants).singleElement().satisfies(plant -> {
                assertThat(plant.usdaSymbol()).isEqualTo("ZEMA");
                assertThat(plant.scientificName()).isEqualTo("Zea mays");
                assertThat(plant.family()).isEqualTo("Poaceae");
            });
        }

        @Test
        @DisplayName("Fails with USDA's error when no copy has the crops")
        void searchWithoutCopyFails() {

            given(usda.searchPlants(anyList())).willThrow(new IllegalStateException("USDA down"));
            List<Crop> unknownCrops = List.of(new Crop("Unknown", "XXXX", false));

            FilePlantSearchCache cache = newCache();

            assertThatThrownBy(() -> cache.searchPlants(unknownCrops))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("USDA down");
        }

        @Test
        @DisplayName("Keeps answering from memory what USDA already returned")
        void memoryStillWorksWhenUsdaFailsLater() {

            given(usda.searchPlants(List.of(CORN)))
                .willReturn(List.of(CORN_PLANT))
                .willThrow(new IllegalStateException("USDA down"));
            FilePlantSearchCache cache = newCache();

            cache.searchPlants(List.of(CORN));
            List<Plant> second = cache.searchPlants(List.of(CORN));

            assertThat(second).containsExactly(CORN_PLANT);
            verify(usda, times(1)).searchPlants(anyList());
        }
    }

    @Test
    @DisplayName("A failed file write doesn't fail the search")
    void failedWriteStillReturnsPlants() throws Exception {

        // A folder where the file should be makes the write fail
        Files.createDirectories(file);
        given(usda.searchPlants(List.of(CORN))).willReturn(List.of(CORN_PLANT));

        List<Plant> plants = newCache().searchPlants(List.of(CORN));

        assertThat(plants).containsExactly(CORN_PLANT);
    }
}
