package com.sequenceiq.sdx.api.model;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxAwsBase {

    @Valid
    @Schema(description = ModelDescriptions.SPOT_PARAMETERS)
    private SdxAwsSpotParameters spot;

    public SdxAwsSpotParameters getSpot() {
        return spot;
    }

    public void setSpot(SdxAwsSpotParameters spot) {
        this.spot = spot;
    }
}
