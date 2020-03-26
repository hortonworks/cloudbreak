package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request;

import javax.validation.constraints.NotNull;

import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupAdjustmentModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class InstanceGroupAdjustmentV4Request implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = InstanceGroupModelDescription.INSTANCE_GROUP_NAME, required = true)
    private String instanceGroup;

    @NotNull
    @ApiModelProperty(value = InstanceGroupAdjustmentModelDescription.SCALING_ADJUSTMENT, required = true)
    private Integer scalingAdjustment;

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
