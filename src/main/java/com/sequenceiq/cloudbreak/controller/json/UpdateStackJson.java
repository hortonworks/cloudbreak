package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.controller.validation.ValidUpdateStackRequest;
import com.sequenceiq.cloudbreak.domain.StatusRequest;

@ValidUpdateStackRequest
public class UpdateStackJson implements JsonEntity {

    private StatusRequest status;

    private HostGroupAdjustmentJson hostGroupAdjustment;

    public UpdateStackJson() {

    }

    public StatusRequest getStatus() {
        return status;
    }

    public void setStatus(StatusRequest status) {
        this.status = status;
    }

    public HostGroupAdjustmentJson getHostGroupAdjustment() {
        return hostGroupAdjustment;
    }

    public void setHostGroupAdjustment(HostGroupAdjustmentJson hostGroupAdjustment) {
        this.hostGroupAdjustment = hostGroupAdjustment;
    }
}