package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import com.henribdev.nasa.challenge.astrasoil.application.ports.output.PlantSearchOutputPort;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Crop;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropList;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlantUsecase")
class PlantUsecaseTests {

    private final PlantSearchOutputPort plantSearch = mock(PlantSearchOutputPort.class);

    @Test
    @DisplayName("Searches the crop list's plants when there is a crop list (demo)")
    void getPlantsSearchesCropList() {

        List<Crop> crops = List.of(new Crop("Corn", "ZEMA", false));
        List<Plant> plants = List.of(new Plant("Corn", "ZEMA", false, "Zea mays", "Poaceae", null));
        given(plantSearch.searchPlants(crops)).willReturn(plants);

        PlantUsecase usecase = new PlantUsecase(plantSearch, Optional.of(new CropList(crops)));

        assertThat(usecase.getPlants()).isEqualTo(plants);
    }

    @Test
    @DisplayName("Returns no plants, without searching, when there's no crop list")
    void getPlantsWithoutCropListReturnsEmpty() {

        PlantUsecase usecase = new PlantUsecase(plantSearch, Optional.empty());

        assertThat(usecase.getPlants()).isEmpty();
        verifyNoInteractions(plantSearch);
    }
}
