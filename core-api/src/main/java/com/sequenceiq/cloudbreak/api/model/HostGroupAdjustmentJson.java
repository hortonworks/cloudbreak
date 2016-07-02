package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupAdjustmentModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("HostGroupAdjustment")
public class HostGroupAdjustmentJson {

    @ApiModelProperty(value = HostGroupModelDescription.HOST_GROUP_NAME, required = true)
    private String hostGroup;
    @ApiModelProperty(value = HostGroupAdjustmentModelDescription.SCALING_ADJUSTMENT, required = true)
    private Integer scalingAdjustment;
    @ApiModelProperty(HostGroupAdjustmentModelDescription.WITH_STACK_UPDATE)
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

    public Boolean getWithStackUpdate() {
        return withStackUpdate;
    }

    public void setWithStackUpdate(Boolean withStackUpdate) {
        this.withStackUpdate = withStackUpdate;
    }

}
