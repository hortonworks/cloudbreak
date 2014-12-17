package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.cluster.flow.StackDependentPollerObject;

public class GccResourceReadyPollerObject extends StackDependentPollerObject {

    private Compute.ZoneOperations.Get zoneOperations;
    private String name;
    private String operationName;


    public GccResourceReadyPollerObject(Compute.ZoneOperations.Get zoneOperations, Stack stack, String name, String operationName) {
        super(stack);
        this.zoneOperations = zoneOperations;
        this.name = name;
        this.operationName = operationName;
    }

    public Compute.ZoneOperations.Get getZoneOperations() {
        return zoneOperations;
    }

    public String getName() {
        return name;
    }

    public String getOperationName() {
        return operationName;
    }
}
