package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupAdjustmentModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.validation.ValidUpdateStackRequest;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@ValidUpdateStackRequest
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateStackV4Request implements JsonEntity {

    @Schema(description = StackModelDescription.STATUS_REQUEST)
    private StatusRequest status;

    @Schema(description = InstanceGroupAdjustmentModelDescription.WITH_CLUSTER_EVENT)
    private Boolean withClusterEvent = Boolean.FALSE;

    @Schema(description = StackModelDescription.INSTANCE_GROUP_ADJUSTMENT)
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

    @Override
    public String toString() {
        return "UpdateStackV4Request{" +
                "status=" + status +
                ", withClusterEvent=" + withClusterEvent +
                ", instanceGroupAdjustment=" + instanceGroupAdjustment +
                '}';
    }
}
