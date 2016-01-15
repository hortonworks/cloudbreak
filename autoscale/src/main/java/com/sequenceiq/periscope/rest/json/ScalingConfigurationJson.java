package com.sequenceiq.periscope.rest.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScalingConfigurationJson implements Json {

    private int minSize;
    private int maxSize;
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
