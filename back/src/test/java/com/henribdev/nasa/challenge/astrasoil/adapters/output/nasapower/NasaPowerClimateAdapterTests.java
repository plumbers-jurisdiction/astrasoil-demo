package com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower.dtos.NasaPowerDailyResponseDto;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import feign.FeignException;
import feign.Request;

// The Feign client is mocked with responses shaped like POWER's
@DisplayName("NasaPowerClimateAdapter")
class NasaPowerClimateAdapterTests {

    private static final String PARAMETERS = "T2M,T2M_MAX,T2M_MIN,PRECTOTCORR,EVPTRNS";
    private static final LocalDate FIRST_DAY = LocalDate.of(2025, 7, 15);
    private static final LocalDate LAST_DAY = LocalDate.of(2025, 7, 16);

    private NasaPowerFeignClient power;
    private NasaPowerClimateAdapter adapter;

    @BeforeEach
    void setUp() {
        power = mock(NasaPowerFeignClient.class);
        // No wait between retries, so the tests run instantly
        adapter = new NasaPowerClimateAdapter(power, 3, 0);
    }

    private static NasaPowerDailyResponseDto response() {
        return new NasaPowerDailyResponseDto(new NasaPowerDailyResponseDto.Properties(Map.of(
            "T2M", Map.of("20250715", 25.03, "20250716", 24.1),
            "T2M_MAX", Map.of("20250715", 31.2, "20250716", 30.0),
            "T2M_MIN", Map.of("20250715", 18.9, "20250716", -999.0),
            "PRECTOTCORR", Map.of("20250715", 0.0, "20250716", 12.5),
            "EVPTRNS", Map.of("20250715", 11.76, "20250716", 4.9)
        )));
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.GET, "https://power.example/api", Map.of(), null, StandardCharsets.UTF_8, null);
    }

    private void givenPowerAnswers() {
        given(power.getDailyPoint(anyString(), anyDouble(), anyDouble(), anyString(), anyString())).willReturn(response());
    }

    @Test
    @DisplayName("Asks POWER for all parameters in one request, with yyyyMMdd dates")
    void requestsAllParametersAtOnce() {

        givenPowerAnswers();

        adapter.searchDailyWeather(42.0, -93.6, FIRST_DAY, LAST_DAY);

        verify(power).getDailyPoint(PARAMETERS, 42.0, -93.6, "20250715", "20250716");
    }

    @Test
    @DisplayName("Turns the values per parameter into one entry per day, in date order")
    void groupsValuesByDay() {

        givenPowerAnswers();

        List<DailyWeather> days = adapter.searchDailyWeather(42.0, -93.6, FIRST_DAY, LAST_DAY);

        assertThat(days).extracting(DailyWeather::date).containsExactly(FIRST_DAY, LAST_DAY);
        assertThat(days.getFirst()).satisfies(day -> {
            assertThat(day.meanTemperature()).isEqualTo(25.03);
            assertThat(day.maxTemperature()).isEqualTo(31.2);
            assertThat(day.minTemperature()).isEqualTo(18.9);
            assertThat(day.rainfall()).isZero();
        });
    }

    @Test
    @DisplayName("Turns evapotranspiration from MJ/m² into mm (divided by 2.45)")
    void convertsEvapotranspirationToMillimetres() {

        givenPowerAnswers();

        List<DailyWeather> days = adapter.searchDailyWeather(42.0, -93.6, FIRST_DAY, LAST_DAY);

        assertThat(days.getFirst().evapotranspiration()).isEqualTo(4.8);
        assertThat(days.get(1).evapotranspiration()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Turns POWER's -999 (no data) into null")
    void fillValueBecomesNull() {

        givenPowerAnswers();

        List<DailyWeather> days = adapter.searchDailyWeather(42.0, -93.6, FIRST_DAY, LAST_DAY);

        assertThat(days.get(1).minTemperature()).isNull();
        assertThat(days.get(1).meanTemperature()).isEqualTo(24.1);
    }

    @Nested
    @DisplayName("When POWER fails")
    class PowerFails {

        @Test
        @DisplayName("Tries again after a server error")
        void retriesServerError() {

            given(power.getDailyPoint(anyString(), anyDouble(), anyDouble(), anyString(), anyString()))
                .willThrow(new FeignException.ServiceUnavailable("Server Side Error", request(), null, null))
                .willReturn(response());

            List<DailyWeather> days = adapter.searchDailyWeather(42.0, -93.6, FIRST_DAY, LAST_DAY);

            assertThat(days).hasSize(2);
            verify(power, times(2)).getDailyPoint(anyString(), anyDouble(), anyDouble(), anyString(), anyString());
        }

        @Test
        @DisplayName("Gives up after 3 attempts")
        void givesUpAfterThreeAttempts() {

            given(power.getDailyPoint(anyString(), anyDouble(), anyDouble(), anyString(), anyString()))
                .willThrow(new FeignException.ServiceUnavailable("Server Side Error", request(), null, null));

            assertThatThrownBy(() -> adapter.searchDailyWeather(42.0, -93.6, FIRST_DAY, LAST_DAY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NASA POWER request failed");
            verify(power, times(3)).getDailyPoint(anyString(), anyDouble(), anyDouble(), anyString(), anyString());
        }

        @Test
        @DisplayName("Doesn't retry a bad request")
        void doesNotRetryBadRequest() {

            given(power.getDailyPoint(anyString(), anyDouble(), anyDouble(), anyString(), anyString()))
                .willThrow(new FeignException.UnprocessableEntity("Invalid parameters", request(), null, null));

            assertThatThrownBy(() -> adapter.searchDailyWeather(42.0, -93.6, FIRST_DAY, LAST_DAY))
                .isInstanceOf(IllegalStateException.class);
            verify(power, times(1)).getDailyPoint(anyString(), anyDouble(), anyDouble(), anyString(), anyString());
        }

        @Test
        @DisplayName("Uses the configured number of attempts")
        void usesConfiguredAttempts() {

            NasaPowerClimateAdapter singleAttempt = new NasaPowerClimateAdapter(power, 1, 0);
            given(power.getDailyPoint(anyString(), anyDouble(), anyDouble(), anyString(), anyString()))
                .willThrow(new FeignException.ServiceUnavailable("Server Side Error", request(), null, null));

            assertThatThrownBy(() -> singleAttempt.searchDailyWeather(42.0, -93.6, FIRST_DAY, LAST_DAY))
                .isInstanceOf(IllegalStateException.class);
            verify(power, times(1)).getDailyPoint(anyString(), anyDouble(), anyDouble(), anyString(), anyString());
        }
    }

    @Test
    @DisplayName("Refuses fewer than 1 attempt or a negative wait")
    void refusesInvalidRetrySettings() {

        assertThatThrownBy(() -> new NasaPowerClimateAdapter(power, 0, 1000))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NasaPowerClimateAdapter(power, 3, -1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
