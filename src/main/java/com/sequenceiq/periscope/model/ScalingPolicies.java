package com.sequenceiq.periscope.model;

import java.util.List;

public class ScalingPolicies {

    private int minSize;
    private int maxSize;
    private int coolDown;
    private List<ScalingPolicy> scalingPolicies;

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

    public List<ScalingPolicy> getScalingPolicies() {
        return scalingPolicies;
    }

    public void setScalingPolicies(List<ScalingPolicy> scalingPolicies) {
        this.scalingPolicies = scalingPolicies;
    }

}
