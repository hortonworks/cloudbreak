package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.HostGroupAdjustmentModelDescription;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.HostGroupModelDescription;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

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
