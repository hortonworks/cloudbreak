package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.Stack;

public class GccRemoveReadyPollerObject {

    private Compute.ZoneOperations.Get zoneOperations;
    private Compute.GlobalOperations.Get globalOperations;
    private Stack stack;
    private String name;

    public GccRemoveReadyPollerObject(Compute.ZoneOperations.Get zoneOperations, Compute.GlobalOperations.Get globalOperations, Stack stack, String name) {
        this.stack = stack;
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

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }
}
