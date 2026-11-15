package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// One row of the soil query, by its SSURGO column names (_r = the survey's representative value).
// Soil Data Access sends rows as lists of text; each becomes a column name -> value map, then this record.
// Values stay text here: the mapper turns them into numbers and yes/no, and odd values into null.
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsdaSsurgoRowDto(

    // Map unit (table mapunit)
    @JsonProperty("muname") String mapUnitName,

    // Component (table component)
    @JsonProperty("cokey") String componentKey,
    @JsonProperty("compname") String componentName,
    @JsonProperty("comppct_r") String componentPercent,
    @JsonProperty("majcompflag") String majorComponent,
    @JsonProperty("drainagecl") String drainageClass,

    // Surface layer (table chorizon, the layer starting at depth 0)
    @JsonProperty("chkey") String layerKey,
    @JsonProperty("om_r") String organicMatter,
    @JsonProperty("ph1to1h2o_r") String ph,
    @JsonProperty("awc_r") String availableWaterCapacity,
    @JsonProperty("kwfact") String erodibility,
    @JsonProperty("sandtotal_r") String sand,
    @JsonProperty("silttotal_r") String silt,
    @JsonProperty("claytotal_r") String clay,

    // Layer texture (table chtexturegrp)
    @JsonProperty("texdesc") String texture,
    @JsonProperty("rvindicator") String representative,
    @JsonProperty("stratextsflag") String stratified
) {}
