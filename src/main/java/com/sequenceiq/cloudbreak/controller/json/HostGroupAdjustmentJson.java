package com.sequenceiq.cloudbreak.controller.json;

public class HostGroupAdjustmentJson {

    private String hostGroup;
    private Integer scalingAdjustment;

    public HostGroupAdjustmentJson() {

    }

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setScalingAdjustment(Integer scalingAdjustment) {
        this.scalingAdjustment = scalingAdjustment;
    }
}
