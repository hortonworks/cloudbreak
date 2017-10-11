package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.validation.ValidUpdateStackRequestV2;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("UpdateStackV2")
@ValidUpdateStackRequestV2
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateStackRequestV2 implements JsonEntity {

    @ApiModelProperty(StackModelDescription.STATUS_REQUEST)
    private StatusRequest status;

    @ApiModelProperty(ModelDescriptions.InstanceGroupAdjustmentModelDescription.WITH_CLUSTER_EVENT)
    private Boolean withClusterEvent = Boolean.FALSE;

    @ApiModelProperty(StackModelDescription.INSTANCE_GROUP_ADJUSTMENT)
    private InstanceGroupAdjustmentJsonV2 instanceGroupAdjustment;

    private Long stackId;

    public StatusRequest getStatus() {
        return status;
    }

    public void setStatus(StatusRequest status) {
        this.status = status;
    }

    public InstanceGroupAdjustmentJsonV2 getInstanceGroupAdjustment() {
        return instanceGroupAdjustment;
    }

    public void setInstanceGroupAdjustment(InstanceGroupAdjustmentJsonV2 instanceGroupAdjustment) {
        this.instanceGroupAdjustment = instanceGroupAdjustment;
    }

    public void setWithClusterEvent(Boolean withClusterEvent) {
        this.withClusterEvent = withClusterEvent;
    }

    public Boolean getWithClusterEvent() {
        return withClusterEvent;
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