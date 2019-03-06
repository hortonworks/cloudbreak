package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupAdjustmentModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.validation.ValidUpdateStackRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidUpdateStackRequest
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateStackV4Request implements JsonEntity {

    @ApiModelProperty(value = StackModelDescription.STATUS_REQUEST, allowableValues = "SYNC,FULL_SYNC,REPAIR_FAILED_NODES,STOPPED,STARTED")
    private StatusRequest status;

    @ApiModelProperty(InstanceGroupAdjustmentModelDescription.WITH_CLUSTER_EVENT)
    private Boolean withClusterEvent = Boolean.FALSE;

    @ApiModelProperty(StackModelDescription.INSTANCE_GROUP_ADJUSTMENT)
    private InstanceGroupAdjustmentV4Request instanceGroupAdjustment;

    public StatusRequest getStatus() {
        return status;
    }

    public void setStatus(StatusRequest status) {
        this.status = status;
    }

    public InstanceGroupAdjustmentV4Request getInstanceGroupAdjustment() {
        return instanceGroupAdjustment;
    }

    public void setInstanceGroupAdjustment(InstanceGroupAdjustmentV4Request instanceGroupAdjustment) {
        this.instanceGroupAdjustment = instanceGroupAdjustment;
    }

    public void setWithClusterEvent(Boolean withClusterEvent) {
        this.withClusterEvent = withClusterEvent;
    }

    public Boolean getWithClusterEvent() {
        return withClusterEvent;
    }
}