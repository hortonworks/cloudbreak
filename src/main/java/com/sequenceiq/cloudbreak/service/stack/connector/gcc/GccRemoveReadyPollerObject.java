package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackDependentPollerObject;

public class GccRemoveReadyPollerObject extends StackDependentPollerObject {

    private Compute.ZoneOperations.Get zoneOperations;
    private Compute.GlobalOperations.Get globalOperations;
    private String name;
    private String operationName;
    private ResourceType resourceType;

    public GccRemoveReadyPollerObject(Compute.ZoneOperations.Get zoneOperations, Compute.GlobalOperations.Get globalOperations,
            Stack stack, String name, String operationName, ResourceType resourceType) {
        super(stack);
        this.name = name;
        this.zoneOperations = zoneOperations;
        this.globalOperations = globalOperations;
        this.operationName = operationName;
        this.resourceType = resourceType;
    }

    public Compute.ZoneOperations.Get getZoneOperations() {
        return zoneOperations;
    }

    public Compute.GlobalOperations.Get getGlobalOperations() {
        return globalOperations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOperationName() {
        return operationName;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }
}
