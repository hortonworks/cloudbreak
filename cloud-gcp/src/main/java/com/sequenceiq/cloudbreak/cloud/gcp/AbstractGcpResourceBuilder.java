package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.common.api.type.ResourceType;

public abstract class AbstractGcpResourceBuilder implements CloudPlatformAware {

    public static final String OPERATION_ID = "opid";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGcpResourceBuilder.class);

    @Inject
    private GcpResourceNameService resourceNameService;

    @Inject
    private GcpResourceChecker gcpResourceChecker;

    @Override
    public Platform platform() {
        return GcpConstants.GCP_PLATFORM;
    }

    @Override
    public Variant variant() {
        return GcpConstants.GCP_VARIANT;
    }

    public GcpResourceNameService getResourceNameService() {
        return resourceNameService;
    }

    protected List<CloudResourceStatus> checkResources(ResourceType type, GcpContext context, AuthenticatedContext auth, Iterable<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (CloudResource resource : resources) {
            LOGGER.debug("Check {} resource: {}", type, resource);
            try {
                String operationId = resource.getStringParameter(OPERATION_ID);
                Operation operation = gcpResourceChecker.check(context, operationId);
                boolean finished = operation == null || GcpStackUtil.isOperationFinished(operation);
                ResourceStatus successStatus = context.isBuild() ? ResourceStatus.CREATED : ResourceStatus.DELETED;
                result.add(new CloudResourceStatus(resource, finished ? successStatus : ResourceStatus.IN_PROGRESS));
                if (finished) {
                    if (successStatus == ResourceStatus.CREATED) {
                        LOGGER.debug("Creation of {} was successful", resource);
                    } else {
                        LOGGER.debug("Deletion of {} was successful", resource);
                    }
                }
            } catch (Exception e) {
                CloudContext cloudContext = auth.getCloudContext();
                throw new GcpResourceException("Error during status check", type,
                        cloudContext.getName(), cloudContext.getId(), resource.getName(), e);
            }
        }
        return result;
    }

    protected String checkException(GoogleJsonResponseException execute) {
        return execute.getDetails().getMessage();
    }

    protected CloudResource createNamedResource(ResourceType type, String name) {
        return new Builder().type(type).name(name).build();
    }

    protected CloudResource createOperationAwareCloudResource(CloudResource resource, Operation operation) {
        return new Builder()
                .cloudResource(resource)
                .params(Collections.singletonMap(OPERATION_ID, operation.getName()))
                .persistent(false)
                .build();
    }

    protected CloudInstance createOperationAwareCloudInstance(CloudInstance instance, Operation operation) {
        return new CloudInstance(instance.getInstanceId(), instance.getTemplate(), instance.getAuthentication(),
                Collections.singletonMap(OPERATION_ID, operation.getName()));
    }

    protected void exceptionHandler(GoogleJsonResponseException ex, String name, ResourceType resourceType) {
        if (ex.getDetails().get("code").equals(HttpStatus.SC_NOT_FOUND)) {
            LOGGER.debug("Resource {} not found: {}", resourceType, name);
        } else {
            throw new GcpResourceException(ex.getDetails().getMessage(), ex);
        }
    }

    protected GcpResourceChecker getGcpResourceChecker() {
        return gcpResourceChecker;
    }
}
