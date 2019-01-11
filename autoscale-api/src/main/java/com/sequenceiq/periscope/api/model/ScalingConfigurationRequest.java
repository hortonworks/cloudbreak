package com.sequenceiq.periscope.api.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.periscope.api.endpoint.validator.ValidScalingConfiguration;
import com.sequenceiq.periscope.doc.ApiDescription.ScalingConfigurationJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ValidScalingConfiguration
@ApiModel
public class ScalingConfigurationRequest implements Json {

    @ApiModelProperty(ScalingConfigurationJsonProperties.MINSIZE)
    @NotNull
    private int minSize;

    @ApiModelProperty(ScalingConfigurationJsonProperties.MAXSIZE)
    @NotNull
    private int maxSize;

    @ApiModelProperty(ScalingConfigurationJsonProperties.COOLDOWN)
    @JsonProperty("cooldown")
    @NotNull
    @Min(1)
    private int coolDown;

    public ScalingConfigurationRequest() {
    }

    public ScalingConfigurationRequest(int minSize, int maxSize, int coolDown) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.coolDown = coolDown;
    }

    public int getMinSize() {
        return minSize;
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getCoolDown() {
        return coolDown;
    }

    public void setCoolDown(int coolDown) {
        this.coolDown = coolDown;
    }
}
