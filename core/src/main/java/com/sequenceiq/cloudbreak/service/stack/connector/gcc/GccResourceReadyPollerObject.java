package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import com.google.api.services.compute.Compute;
import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class GccResourceReadyPollerObject extends StackContext {

    private Optional<Compute.ZoneOperations.Get> zoneOperations;
    private Optional<Compute.RegionOperations.Get> regionOperations;
    private Optional<Compute.GlobalOperations.Get> globalOperations;
    private String name;
    private String operationName;
    private ResourceType resourceType;

    public GccResourceReadyPollerObject(Compute.ZoneOperations.Get zoneOperations, Stack stack, String name, String operationName, ResourceType resourceType) {
        super(stack);
        this.zoneOperations = Optional.fromNullable(zoneOperations);
        this.regionOperations = Optional.absent();
        this.globalOperations = Optional.absent();
        this.name = name;
        this.operationName = operationName;
        this.resourceType = resourceType;
    }

    public GccResourceReadyPollerObject(Compute.RegionOperations.Get regionOperations, Stack stack, String name, String operationName,
            ResourceType resourceType) {
        super(stack);
        this.regionOperations = Optional.fromNullable(regionOperations);
        this.zoneOperations = Optional.absent();
        this.globalOperations = Optional.absent();
        this.name = name;
        this.operationName = operationName;
        this.resourceType = resourceType;
    }

    public GccResourceReadyPollerObject(Compute.GlobalOperations.Get globalOperations, Stack stack, String name, String operationName,
            ResourceType resourceType) {
        super(stack);
        this.globalOperations = Optional.fromNullable(globalOperations);
        this.zoneOperations = Optional.absent();
        this.regionOperations = Optional.absent();
        this.name = name;
        this.operationName = operationName;
        this.resourceType = resourceType;
    }

    public Optional<Compute.RegionOperations.Get> getRegionOperations() {
        return regionOperations;
    }

    public Optional<Compute.ZoneOperations.Get> getZoneOperations() {
        return zoneOperations;
    }

    public Optional<Compute.GlobalOperations.Get> getGlobalOperations() {
        return globalOperations;
    }

    public String getName() {
        return name;
    }

    public String getOperationName() {
        return operationName;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }
}
