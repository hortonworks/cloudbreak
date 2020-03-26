package com.sequenceiq.distrox.api.v1.distrox.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupAdjustmentModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistroXScaleV1Request implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = InstanceGroupModelDescription.INSTANCE_GROUP_NAME, required = true)
    private String group;

    @NotNull
    @ApiModelProperty(value = InstanceGroupAdjustmentModelDescription.SCALING_ADJUSTMENT, required = true)
    private Integer desiredCount;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Integer getDesiredCount() {
        return desiredCount;
    }

    public void setDesiredCount(Integer desiredCount) {
        this.desiredCount = desiredCount;
    }
}
