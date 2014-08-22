package com.sequenceiq.cloudbreak.controller.json;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.StatusRequest;

public class UpdateClusterJson implements JsonEntity {

    private Set<HostGroupAdjustmentJson> hostGroupAdjustments = new HashSet<>();
    private StatusRequest status;

    public UpdateClusterJson() {

    }

    public StatusRequest getStatus() {
        return status;
    }

    public void setStatus(StatusRequest status) {
        this.status = status;
    }

    public Set<HostGroupAdjustmentJson> getHostGroupAdjustments() {
        return hostGroupAdjustments;
    }

    public void setHostGroupAdjustments(Set<HostGroupAdjustmentJson> hostGroupAdjustments) {
        this.hostGroupAdjustments = hostGroupAdjustments;
    }
}
