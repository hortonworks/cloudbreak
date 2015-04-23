package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.InstanceGroupAdjustmentModelDescription;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel("InstanceGroupAdjustment")
public class InstanceGroupAdjustmentJson {

    @ApiModelProperty(value = InstanceGroupModelDescription.INSTANCE_GROUP_NAME, required = true)
    private String instanceGroup;
    @ApiModelProperty(value = InstanceGroupAdjustmentModelDescription.SCALING_ADJUSTMENT, required = true)
    private Integer scalingAdjustment;
    @ApiModelProperty(value = InstanceGroupAdjustmentModelDescription.WITH_CLUSTER_EVENT)
    private Boolean withClusterEvent = false;

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

    public void setWithClusterEvent(Boolean withClusterEvent) {
        this.withClusterEvent = withClusterEvent;
    }

    public Boolean getWithClusterEvent() {
        return withClusterEvent;
    }
}
