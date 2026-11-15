package com.henribdev.nasa.challenge.astrasoil.domain.services.soil;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilComponent;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilShare;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilTexture;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SurfaceLayer;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.TextureGroup;

// Turns the soil survey's map unit into the soil summary the rules read.
// - The rules use the dominant component: the single row with the largest share (NRCS's "dominant component" method).
//   Rows that share a name are not added up here.
// - The farmer is told the mix instead: rows of the same name added together, largest first, small ones left out.
public final class SoilSummarizer {

    // A soil under this share of the map unit is left out of the mix text
    private static final int MIX_MIN_SHARE_PERCENT = 25;

    // USDA PLANTS' texture classes and their groups (the table printed under the "Adapted to ... Textured Soils"
    // definitions), the same for every crop
    private static final Map<String, TextureGroup> TEXTURE_CLASSES = Map.ofEntries(
        Map.entry("sand", TextureGroup.COARSE),
        Map.entry("coarse sand", TextureGroup.COARSE),
        Map.entry("fine sand", TextureGroup.COARSE),
        Map.entry("very fine sand", TextureGroup.COARSE),
        Map.entry("loamy sand", TextureGroup.COARSE),
        Map.entry("loamy coarse sand", TextureGroup.COARSE),
        Map.entry("loamy fine sand", TextureGroup.COARSE),
        Map.entry("loamy very fine sand", TextureGroup.COARSE),
        Map.entry("sandy loam", TextureGroup.MEDIUM),
        Map.entry("coarse sandy loam", TextureGroup.MEDIUM),
        Map.entry("fine sandy loam", TextureGroup.MEDIUM),
        Map.entry("very fine sandy loam", TextureGroup.MEDIUM),
        Map.entry("loam", TextureGroup.MEDIUM),
        Map.entry("silt loam", TextureGroup.MEDIUM),
        Map.entry("silt", TextureGroup.MEDIUM),
        Map.entry("sandy clay loam", TextureGroup.MEDIUM),
        Map.entry("clay loam", TextureGroup.MEDIUM),
        Map.entry("silty clay loam", TextureGroup.MEDIUM),
        Map.entry("sandy clay", TextureGroup.FINE),
        Map.entry("silty clay", TextureGroup.FINE),
        Map.entry("clay", TextureGroup.FINE)
    );

    private SoilSummarizer() {}

    // Empty when the map unit has no components
    public static Optional<SoilSummary> summarize(SoilData soil) {

        // On equal shares, the first row stays (the survey lists them largest first)
        Optional<SoilComponent> dominant = soil.components().stream().max(Comparator.comparingInt(SoilSummarizer::share));
        if (dominant.isEmpty()) {
            return Optional.empty();
        }

        SoilComponent component = dominant.get();
        SurfaceLayer layer = component.surfaceLayer();
        Optional<SoilTexture> texture = layer == null ? Optional.empty() : representativeTexture(layer.textures());
        // A stratified layer is thin bands of different textures: no single group fits it
        TextureGroup group = texture.filter(found -> !found.stratified()).flatMap(found -> textureGroup(found.name())).orElse(null);

        return Optional.of(new SoilSummary(
            component.name(),
            component.sharePercent(),
            component.drainageClass(),
            layer == null ? null : layer.organicMatterPercent(),
            layer == null ? null : layer.ph(),
            layer == null ? null : layer.availableWaterCapacity(),
            layer == null ? null : layer.erodibility(),
            texture.map(SoilTexture::name).orElse(null),
            group,
            mix(soil.components())
        ));
    }

    // The survey's typical texture for the layer
    private static Optional<SoilTexture> representativeTexture(List<SoilTexture> textures) {
        return textures == null ? Optional.empty() : textures.stream().filter(SoilTexture::representative).findFirst();
    }

    // The group of the longest class name the texture ends with, which strips modifiers:
    // "Gravelly fine sandy loam" -> "fine sandy loam" -> medium. Empty when no class fits (e.g. muck, bedrock)
    static Optional<TextureGroup> textureGroup(String texture) {
        if (texture == null) {
            return Optional.empty();
        }
        String name = texture.trim().toLowerCase(Locale.ROOT);
        return TEXTURE_CLASSES.keySet().stream()
            .filter(textureClass -> name.equals(textureClass) || name.endsWith(" " + textureClass))
            .max(Comparator.comparingInt(String::length))
            .map(TEXTURE_CLASSES::get);
    }

    private static List<SoilShare> mix(List<SoilComponent> components) {
        Map<String, Integer> shareByName = new LinkedHashMap<>();
        for (SoilComponent component : components) {
            if (component.name() != null) {
                shareByName.merge(component.name(), share(component), Integer::sum);
            }
        }
        return shareByName.entrySet().stream()
            .filter(entry -> entry.getValue() >= MIX_MIN_SHARE_PERCENT)
            .map(entry -> new SoilShare(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingInt(SoilShare::sharePercent).reversed())
            .toList();
    }

    private static int share(SoilComponent component) {
        return Objects.requireNonNullElse(component.sharePercent(), 0);
    }
}
