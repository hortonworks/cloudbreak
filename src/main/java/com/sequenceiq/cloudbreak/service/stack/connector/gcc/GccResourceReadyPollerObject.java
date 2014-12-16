package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.Stack;

public class GccResourceReadyPollerObject {

    private Compute.ZoneOperations.Get zoneOperations;
    private Stack stack;
    private String name;


    public GccResourceReadyPollerObject(Compute.ZoneOperations.Get zoneOperations, Stack stack, String name) {
        this.zoneOperations = zoneOperations;
        this.stack = stack;
        this.name = name;
    }

    public Compute.ZoneOperations.Get getZoneOperations() {
        return zoneOperations;
    }

    public Stack getStack() {
        return stack;
    }

    public String getName() {
        return name;
    }
}
