package com.sequenceiq.cloudbreak.controller.json;

import javax.validation.constraints.Digits;

import com.sequenceiq.cloudbreak.controller.validation.ValidUpdateStackRequest;
import com.sequenceiq.cloudbreak.domain.StatusRequest;

@ValidUpdateStackRequest
public class UpdateStackJson implements JsonEntity {

    private StatusRequest status;

    @Digits(fraction = 0, integer = 10, message = "The node count has to be a number")
    private Integer scalingAdjustment;

    public UpdateStackJson() {

    }

    public StatusRequest getStatus() {
        return status;
    }

    public void setStatus(StatusRequest status) {
        this.status = status;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setScalingAdjustment(Integer scalingAdjustment) {
        this.scalingAdjustment = scalingAdjustment;
    }
}