package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.ResourceType;

public abstract class AbstractGcpComputeBaseResourceChecker extends AbstractGcpBaseResourceChecker {

    public static final String OPERATION_ID = "opid";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGcpComputeBaseResourceChecker.class);

    @Inject
    private GcpResourceNameService resourceNameService;

    @Inject
    private GcpComputeResourceChecker resourceChecker;

    protected List<CloudResourceStatus> checkResources(
        ResourceType type, GcpContext context, AuthenticatedContext auth, Iterable<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (CloudResource resource : resources) {
            LOGGER.debug("Check {} resource: {}", type, resource);
            try {
                String operationId = resource.getStringParameter(OPERATION_ID);
                Operation operation = resourceChecker.check(context, operationId);
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

    public GcpResourceNameService getResourceNameService() {
        return resourceNameService;
    }

    public GcpComputeResourceChecker getResourceChecker() {
        return resourceChecker;
    }
}
