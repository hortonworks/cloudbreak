package com.sequenceiq.cloudbreak.controller.json;

import java.util.List;

import com.sequenceiq.cloudbreak.controller.validation.ValidUpdateStackRequest;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.domain.SubnetJson;

@ValidUpdateStackRequest
public class UpdateStackJson implements JsonEntity {

    private StatusRequest status;

    private HostGroupAdjustmentJson hostGroupAdjustment;

    private List<SubnetJson> allowedSubnets;

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

    public List<SubnetJson> getAllowedSubnets() {
        return allowedSubnets;
    }

    public void setAllowedSubnets(List<SubnetJson> allowedSubnets) {
        this.allowedSubnets = allowedSubnets;
    }
}