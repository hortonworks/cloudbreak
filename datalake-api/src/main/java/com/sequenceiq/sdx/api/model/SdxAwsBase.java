package com.sequenceiq.sdx.api.model;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxAwsBase {

    @ApiModelProperty(ModelDescriptions.SPOT_PARAMETERS)
    @Valid
    private SdxAwsSpotParameters spot;

    public SdxAwsSpotParameters getSpot() {
        return spot;
    }

    public void setSpot(SdxAwsSpotParameters spot) {
        this.spot = spot;
    }
}
