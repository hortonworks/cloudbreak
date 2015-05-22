package com.sequenceiq.cloudbreak.service.stack.resource.gcp;

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
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.domain.GcpZone;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.UpdateContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpStartStopContextObject;

public abstract class GcpSimpleInstanceResourceBuilder implements
        ResourceBuilder<GcpProvisionContextObject, GcpDeleteContextObject, GcpStartStopContextObject, UpdateContextObject> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(GcpSimpleInstanceResourceBuilder.class);
    protected static final int MAX_POLLING_ATTEMPTS = 60;
    protected static final int POLLING_INTERVAL = 5000;
    protected static final int NOT_FOUND = 404;

    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCP;
    }

    protected Compute.ZoneOperations.Get createZoneOperations(Compute compute, GcpCredential gcpCredential, Operation operation, GcpZone region)
            throws IOException {
        return compute.zoneOperations().get(gcpCredential.getProjectId(), region.getValue(), operation.getName());
    }

    protected Compute.GlobalOperations.Get createGlobalOperations(Compute compute, GcpCredential gcpCredential, Operation operation)
            throws IOException {
        return compute.globalOperations().get(gcpCredential.getProjectId(), operation.getName());
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
    public Boolean start(GcpStartStopContextObject startStopContextObject, Resource resource, String region) {
        return true;
    }

    @Override
    public Boolean stop(GcpStartStopContextObject startStopContextObject, Resource resource, String region) {
        return true;
    }

    @Override
    public Boolean rollback(Resource resource, GcpDeleteContextObject deleteContextObject, String region) throws Exception {
        return delete(resource, deleteContextObject, region);
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.INSTANCE_RESOURCE;
    }
}
