package com.sequenceiq.periscope.domain;

public class ScalingActivityDetails {

    private volatile long lastScalingActivityCompleted;

    private String lastScalingFlowId;

    public long getLastScalingActivityCompleted() {
        return lastScalingActivityCompleted;
    }

    public void setLastScalingActivityCompleted(long lastScalingActivityCompleted) {
        this.lastScalingActivityCompleted = lastScalingActivityCompleted;
    }

    public String getLastScalingFlowId() {
        return lastScalingFlowId;
    }

    public void setLastScalingFlowId(String lastScalingFlowId) {
        this.lastScalingFlowId = lastScalingFlowId;
    }

    @Override
    public String toString() {
        return "ScalingActivityDetails{" +
                "scalingActivityCompleted=" + lastScalingActivityCompleted +
                ", pollableFlowId='" + lastScalingFlowId + '\'' +
                '}';
    }
}
