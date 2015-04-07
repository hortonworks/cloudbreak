package com.sequenceiq.cloudbreak.controller.json;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.domain.StatusRequest;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateClusterJson implements JsonEntity {

    private HostGroupAdjustmentJson hostGroupAdjustment;
    private StatusRequest status;
    private Long blueprintId;
    private Set<HostGroupJson> hostgroups;

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

    public Long getBlueprintId() {
        return blueprintId;
    }

    public Set<HostGroupJson> getHostgroups() {
        return hostgroups;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public void setHostgroups(Set<HostGroupJson> hostgroups) {
        this.hostgroups = hostgroups;
    }
}
