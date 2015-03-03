package com.sequenceiq.cloudbreak.controller.json;

public class InstanceGroupAdjustmentJson {

    private String instanceGroup;
    private Integer scalingAdjustment;

    public InstanceGroupAdjustmentJson() {

    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setScalingAdjustment(Integer scalingAdjustment) {
        this.scalingAdjustment = scalingAdjustment;
    }
}
