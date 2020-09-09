package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.sqladmin.SQLAdmin;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.ResourceType;

public abstract class AbstractGcpDatabaseBaseResourceChecker extends AbstractGcpBaseResourceChecker {

    public static final String OPERATION_ID = "opid";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGcpDatabaseBaseResourceChecker.class);

    @Inject
    private GcpDatabaseResourceChecker gcpResourceChecker;

    public List<CloudResourceStatus> checkResources(
            ResourceType type, SQLAdmin sqlAdmin, AuthenticatedContext auth, Iterable<CloudResource> resources, ResourceStatus waitedStatus) {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (CloudResource resource : resources) {
            LOGGER.debug("Check {} resource: {}", type, resource);
            try {
                String operationId = resource.getStringParameter(OPERATION_ID);
                com.google.api.services.sqladmin.model.Operation operation = gcpResourceChecker.check(sqlAdmin, auth, operationId);
                boolean finished = operation == null || GcpStackUtil.isOperationFinished(operation);
                if (finished) {
                    result.add(new CloudResourceStatus(resource, waitedStatus));
                } else {
                    result.add(new CloudResourceStatus(resource, ResourceStatus.IN_PROGRESS));
                }
            } catch (Exception e) {
                CloudContext cloudContext = auth.getCloudContext();
                throw new GcpResourceException("Error during status check", type,
                        cloudContext.getName(), cloudContext.getId(), resource.getName(), e);
            }
        }
        return result;
    }

    protected CloudResource createOperationAwareCloudResource(CloudResource resource, com.google.api.services.sqladmin.model.Operation operation) {
        return new Builder()
                .cloudResource(resource)
                .params(Collections.singletonMap(OPERATION_ID, operation.getName()))
                .persistent(false)
                .build();
    }
}
