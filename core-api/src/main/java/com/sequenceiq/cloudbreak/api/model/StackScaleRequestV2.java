package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("StackScaleRequestV2")
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackScaleRequestV2 implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.InstanceGroupModelDescription.INSTANCE_GROUP_NAME, required = true)
    private String group;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.InstanceGroupAdjustmentModelDescription.SCALING_ADJUSTMENT, required = true)
    private Integer desiredCount;

    private Long stackId;

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

    @JsonIgnore
    public Long getStackId() {
        return stackId;
    }

    @JsonIgnore
    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }
}