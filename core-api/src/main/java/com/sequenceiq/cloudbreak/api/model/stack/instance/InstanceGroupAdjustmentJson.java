package com.sequenceiq.cloudbreak.api.model.stack.instance;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupAdjustmentModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("InstanceGroupAdjustment")
public class InstanceGroupAdjustmentJson implements JsonEntity {

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

    @Override
    public String toString() {
        return "InstanceGroupAdjustmentJson{"
                + "instanceGroup='" + instanceGroup + '\''
                + ", scalingAdjustment=" + scalingAdjustment
                + '}';
    }
}
