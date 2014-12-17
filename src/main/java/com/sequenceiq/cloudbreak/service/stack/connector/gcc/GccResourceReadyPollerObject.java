package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.Stack;

public class GccResourceReadyPollerObject {

    private Compute.ZoneOperations.Get zoneOperations;
    private Stack stack;
    private String name;
    private String operationName;


    public GccResourceReadyPollerObject(Compute.ZoneOperations.Get zoneOperations, Stack stack, String name, String operationName) {
        this.zoneOperations = zoneOperations;
        this.stack = stack;
        this.name = name;
        this.operationName = operationName;
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

    public String getOperationName() {
        return operationName;
    }
}
