package com.sequenceiq.periscope.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.periscope.doc.ApiDescription.ScalingConfigurationJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ScalingConfigurationJson")
public class ScalingConfigurationJson implements Json {

    @ApiModelProperty(ScalingConfigurationJsonProperties.MINSIZE)
    private int minSize;
    @ApiModelProperty(ScalingConfigurationJsonProperties.MAXSIZE)
    private int maxSize;
    @ApiModelProperty(ScalingConfigurationJsonProperties.COOLDOWN)
    @JsonProperty("cooldown")
    private int coolDown;

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
