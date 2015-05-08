package com.sequenceiq.cloudbreak.service.stack.resource.gcc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GcpResourceException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccZone;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.UpdateContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccStartStopContextObject;

public abstract class GccSimpleInstanceResourceBuilder implements
        ResourceBuilder<GccProvisionContextObject, GccDeleteContextObject, GccStartStopContextObject, UpdateContextObject> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(GccSimpleInstanceResourceBuilder.class);
    protected static final int MAX_POLLING_ATTEMPTS = 60;
    protected static final int POLLING_INTERVAL = 5000;
    protected static final int NOT_FOUND = 404;

    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCC;
    }

    protected Compute.ZoneOperations.Get createZoneOperations(Compute compute, GccCredential gccCredential, Operation operation, GccZone region)
            throws IOException {
        return compute.zoneOperations().get(gccCredential.getProjectId(), region.getValue(), operation.getName());
    }

    protected Compute.GlobalOperations.Get createGlobalOperations(Compute compute, GccCredential gccCredential, Operation operation)
            throws IOException {
        return compute.globalOperations().get(gccCredential.getProjectId(), operation.getName());
    }

    protected void exceptionHandler(GoogleJsonResponseException ex, String name, Stack stack) {
        if (ex.getDetails().get("code").equals(NOT_FOUND)) {
            LOGGER.info(String.format("Resource was delete with name: %s", name));
        } else {
            throw new GcpResourceException("Error while creating instances", ex);
        }
    }

    protected List<Resource> filterResourcesByType(Collection<Resource> resources, ResourceType resourceType) {
        List<Resource> resourcesTemp = new ArrayList<>();
        for (Resource resource : resources) {
            if (resourceType.equals(resource.getResourceType())) {
                resourcesTemp.add(resource);
            }
        }
        return resourcesTemp;
    }

    @Override
    public void update(UpdateContextObject updateContextObject) {
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
        return ResourceBuilderType.INSTANCE_RESOURCE;
    }
}
