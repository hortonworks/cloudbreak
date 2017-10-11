package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupAdjustmentModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ClusterScaleRequestV2")
public class ClusterScaleRequestV2 implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = HostGroupModelDescription.HOST_GROUP_NAME, required = true)
    private String group;

    @NotNull
    @ApiModelProperty(value = HostGroupAdjustmentModelDescription.SCALING_ADJUSTMENT, required = true)
    private Integer desiredCount;

    @ApiModelProperty(HostGroupAdjustmentModelDescription.WITH_STACK_UPDATE)
    private Boolean withStackUpdate = Boolean.TRUE;

    @ApiModelProperty(HostGroupAdjustmentModelDescription.VALIDATE_NODE_COUNT)
    private Boolean validateNodeCount = Boolean.TRUE;

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

    public Boolean getWithStackUpdate() {
        return withStackUpdate;
    }

    public void setWithStackUpdate(Boolean withStackUpdate) {
        this.withStackUpdate = withStackUpdate;
    }

    public Boolean getValidateNodeCount() {
        return validateNodeCount;
    }

    public void setValidateNodeCount(Boolean validateNodeCount) {
        this.validateNodeCount = validateNodeCount;
    }
}
