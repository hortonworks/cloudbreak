package com.sequenceiq.cloudbreak.service.stack.resource.gcc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GcpResourceException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccZone;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccStartStopContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccUpdateContextObject;

public abstract class GccSimpleNetworkResourceBuilder implements
        ResourceBuilder<GccProvisionContextObject, GccDeleteContextObject, GccStartStopContextObject, GccUpdateContextObject> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(GccSimpleNetworkResourceBuilder.class);
    protected static final int MAX_POLLING_ATTEMPTS = 60;
    protected static final int POLLING_INTERVAL = 5000;
    private static final int NOT_FOUND = 404;

    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCC;
    }

    protected Compute.ZoneOperations.Get createZoneOperations(Compute compute, GccCredential gccCredential, Operation operation, GccZone region)
            throws IOException {
        return compute.zoneOperations().get(gccCredential.getProjectId(), region.getValue(), operation.getName());
    }

    protected Compute.RegionOperations.Get createRegionOperations(Compute compute, GccCredential gccCredential, Operation operation, GccZone region)
            throws IOException {
        return compute.regionOperations().get(gccCredential.getProjectId(), region.getRegion(), operation.getName());
    }

    protected Compute.GlobalOperations.Get createGlobalOperations(Compute compute, GccCredential gccCredential, Operation operation)
            throws IOException {
        return compute.globalOperations().get(gccCredential.getProjectId(), operation.getName());
    }

    protected void exceptionHandler(GoogleJsonResponseException ex, String name, Stack stack) {
        if (ex.getDetails().get("code").equals(NOT_FOUND)) {
            LOGGER.info(String.format("Resource not found. Resource name: %s ", name));
        } else {
            throw new GcpResourceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void update(GccUpdateContextObject updateContextObject) {
    }

    @Override
    public Boolean start(GccStartStopContextObject startStopContextObject, Resource resource, String region) {
        return true;
    }

    @Override
    public Boolean stop(GccStartStopContextObject startStopContextObject, Resource resource, String region) {
        return true;
    }

    @Override
    public Boolean rollback(Resource resource, GccDeleteContextObject deleteContextObject, String region) throws Exception {
        return delete(resource, deleteContextObject, region);
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.NETWORK_RESOURCE;
    }
}
