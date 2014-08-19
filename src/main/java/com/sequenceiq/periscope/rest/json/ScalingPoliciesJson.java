package com.sequenceiq.periscope.rest.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScalingPoliciesJson implements Json {

    private int minSize;
    private int maxSize;
    @JsonProperty("cooldown")
    private int coolDown;
    private List<ScalingPolicyJson> scalingPolicies;

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

    public List<ScalingPolicyJson> getScalingPolicies() {
        return scalingPolicies;
    }

    public void setScalingPolicies(List<ScalingPolicyJson> scalingPolicies) {
        this.scalingPolicies = scalingPolicies;
    }
}
