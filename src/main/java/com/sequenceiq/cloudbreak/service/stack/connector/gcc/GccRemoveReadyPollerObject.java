package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import com.google.api.services.compute.Compute;

public class GccRemoveReadyPollerObject {

    private Compute.ZoneOperations.Get zoneOperations;
    private Compute.GlobalOperations.Get globalOperations;
    private Long stackId;
    private String name;

    public GccRemoveReadyPollerObject(Compute.ZoneOperations.Get zoneOperations, Compute.GlobalOperations.Get globalOperations, Long stackId, String name) {
        this.stackId = stackId;
        this.name = name;
        this.zoneOperations = zoneOperations;
        this.globalOperations = globalOperations;
    }

    public Compute.ZoneOperations.Get getZoneOperations() {
        return zoneOperations;
    }

    public void setZoneOperations(Compute.ZoneOperations.Get zoneOperations) {
        this.zoneOperations = zoneOperations;
    }

    public Compute.GlobalOperations.Get getGlobalOperations() {
        return globalOperations;
    }

    public void setGlobalOperations(Compute.GlobalOperations.Get globalOperations) {
        this.globalOperations = globalOperations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }
}
