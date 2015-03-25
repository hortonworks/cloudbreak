package com.sequenceiq.cloudbreak.controller.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.controller.validation.ValidUpdateStackRequest;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel("UpdateStack")
@ValidUpdateStackRequest
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateStackJson implements JsonEntity {

    @ApiModelProperty(required = true)
    private StatusRequest status;

    private InstanceGroupAdjustmentJson instanceGroupAdjustment;

    private List<SubnetJson> allowedSubnets;

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

    public List<SubnetJson> getAllowedSubnets() {
        return allowedSubnets;
    }

    public void setAllowedSubnets(List<SubnetJson> allowedSubnets) {
        this.allowedSubnets = allowedSubnets;
    }
}