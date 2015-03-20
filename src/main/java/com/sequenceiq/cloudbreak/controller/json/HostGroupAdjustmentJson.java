package com.sequenceiq.cloudbreak.controller.json;

public class HostGroupAdjustmentJson {

    private String hostGroup;
    private Integer scalingAdjustment;
    private Boolean withStackUpdate = false;

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

    public Boolean isWithStackUpdate() {
        return withStackUpdate;
    }

    public void setWithStackUpdate(Boolean withStackUpdate) {
        this.withStackUpdate = withStackUpdate;
    }

    public Boolean getWithStackUpdate() {
        return withStackUpdate;
    }
}
