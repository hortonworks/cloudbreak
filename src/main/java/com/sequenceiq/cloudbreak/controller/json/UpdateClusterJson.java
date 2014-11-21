package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.domain.StatusRequest;

public class UpdateClusterJson implements JsonEntity {

    private HostGroupAdjustmentJson hostGroupAdjustment;
    private StatusRequest status;

    public UpdateClusterJson() {
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
