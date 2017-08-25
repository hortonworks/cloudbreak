package com.sequenceiq.periscope.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.periscope.doc.ApiDescription.ScalingConfigurationJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ScalingConfiguration")
public class ScalingConfigurationJson implements Json {

    @ApiModelProperty(ScalingConfigurationJsonProperties.MINSIZE)
    @NotNull
    private int minSize;

    @ApiModelProperty(ScalingConfigurationJsonProperties.MAXSIZE)
    @NotNull
    private int maxSize;

    @ApiModelProperty(ScalingConfigurationJsonProperties.COOLDOWN)
    @JsonProperty("cooldown")
    @NotNull
    private int coolDown;

    public ScalingConfigurationJson() {
    }

    public ScalingConfigurationJson(int minSize, int maxSize, int coolDown) {
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
