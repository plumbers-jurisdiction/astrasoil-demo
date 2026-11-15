package com.henribdev.nasa.challenge.astrasoil.domain.services.soil;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilComponent;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilShare;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilTexture;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SurfaceLayer;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.TextureGroup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("SoilSummarizer")
class SoilSummarizerTests {

    private static SurfaceLayer layer(SoilTexture... textures) {
        return new SurfaceLayer(2.5, 6.5, 0.17, 0.17, 65.4, 19.6, 15.0, List.of(textures));
    }

    private static SoilComponent component(String name, Integer share, SurfaceLayer layer) {
        return new SoilComponent(name, share, true, "Moderately well drained", layer);
    }

    private static SoilSummary summarize(SoilComponent... components) {
        return SoilSummarizer.summarize(new SoilData("Test map unit", List.of(components))).orElseThrow();
    }

    @Test
    @DisplayName("Reads the dominant component's surface layer, and merges the mix by name (Iowa's map unit)")
    void iowa() {

        SoilSummary soil = summarize(
            component("Hanlon", 60, layer(new SoilTexture("Fine sandy loam", true, false))),
            component("Spillville", 30, layer(new SoilTexture("Loam", true, false))),
            component("Spillville", 5, null),
            component("Coland", 5, layer(new SoilTexture("Clay loam", true, false))));

        assertThat(soil.soilName()).isEqualTo("Hanlon");
        assertThat(soil.sharePercent()).isEqualTo(60);
        assertThat(soil.organicMatterPercent()).isEqualTo(2.5);
        assertThat(soil.texture()).isEqualTo("Fine sandy loam");
        assertThat(soil.textureGroup()).isEqualTo(TextureGroup.MEDIUM);
        // Spillville 30 + 5 = 35; Coland is under 25%
        assertThat(soil.mix()).containsExactly(new SoilShare("Hanlon", 60), new SoilShare("Spillville", 35));
    }

    @Test
    @DisplayName("Uses the representative texture row")
    void representativeTexture() {

        SoilSummary soil = summarize(component("Coland", 100,
            layer(new SoilTexture("Silty clay loam", false, false), new SoilTexture("Clay loam", true, false))));

        assertThat(soil.texture()).isEqualTo("Clay loam");
    }

    @Nested
    @DisplayName("Texture group")
    class Group {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            "Fine sandy loam, MEDIUM",
            "Gravelly fine sandy loam, MEDIUM",
            "Loamy sand, COARSE",
            "Very fine sand, COARSE",
            "Clay loam, MEDIUM",
            "Silty clay, FINE",
            "Very cobbly clay, FINE",
            "silt loam, MEDIUM"
        })
        @DisplayName("Takes the longest class name the texture ends with")
        void longestClass(String texture, TextureGroup group) {
            assertThat(SoilSummarizer.textureGroup(texture)).contains(group);
        }

        @Test
        @DisplayName("Has none for textures that aren't mineral soil classes")
        void noClass() {
            assertThat(SoilSummarizer.textureGroup("Muck")).isEmpty();
            assertThat(SoilSummarizer.textureGroup("Bedrock")).isEmpty();
        }

        @Test
        @DisplayName("Has none for a stratified layer or without a representative texture")
        void notCheckable() {

            SoilSummary stratified = summarize(component("A", 100, layer(new SoilTexture("Loam", true, true))));
            SoilSummary noRepresentative = summarize(component("B", 100, layer(new SoilTexture("Loam", false, false))));

            assertThat(stratified.textureGroup()).isNull();
            assertThat(noRepresentative.texture()).isNull();
            assertThat(noRepresentative.textureGroup()).isNull();
        }
    }

    @Test
    @DisplayName("Keeps the dominant soil's name and drainage even without a surface layer")
    void noSurfaceLayer() {

        SoilSummary soil = summarize(component("Rock outcrop", 80, null));

        assertThat(soil.soilName()).isEqualTo("Rock outcrop");
        assertThat(soil.drainageClass()).isEqualTo("Moderately well drained");
        assertThat(soil.ph()).isNull();
        assertThat(soil.textureGroup()).isNull();
    }

    @Test
    @DisplayName("Is empty for a map unit without components")
    void noComponents() {
        assertThat(SoilSummarizer.summarize(new SoilData("Water", List.of()))).isEmpty();
    }
}
