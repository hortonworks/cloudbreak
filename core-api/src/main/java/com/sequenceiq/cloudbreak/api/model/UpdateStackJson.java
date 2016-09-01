package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.validation.ValidUpdateStackRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("UpdateStack")
@ValidUpdateStackRequest
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateStackJson implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.STATUS_REQUEST)
    private StatusRequest status;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.INSTANCE_GROUP_ADJUSTMENT)
    private InstanceGroupAdjustmentJson instanceGroupAdjustment;

    public UpdateStackJson() {

    }

    public StatusRequest getStatus() {
        return status;
    }

    public void setStatus(StatusRequest status) {
        this.status = status;
    }

    public InstanceGroupAdjustmentJson getInstanceGroupAdjustment() {
        return instanceGroupAdjustment;
    }

    public void setInstanceGroupAdjustment(InstanceGroupAdjustmentJson instanceGroupAdjustment) {
        this.instanceGroupAdjustment = instanceGroupAdjustment;
    }
}