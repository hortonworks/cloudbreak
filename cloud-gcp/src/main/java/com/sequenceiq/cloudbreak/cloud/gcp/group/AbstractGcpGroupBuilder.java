package com.sequenceiq.cloudbreak.cloud.gcp.group;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.ComputeRequest;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.AbstractGcpResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.template.GroupResourceBuilder;
import com.sequenceiq.common.api.type.ResourceType;

public abstract class AbstractGcpGroupBuilder extends AbstractGcpResourceBuilder implements GroupResourceBuilder<GcpContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGcpGroupBuilder.class);

    @Override
    public List<CloudResourceStatus> checkResources(GcpContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(resourceType(), context, auth, resources);
    }

    @Override
    public CloudResourceStatus update(GcpContext context, AuthenticatedContext auth, Group group, Network network,
            Security security, CloudResource resource) {
        return null;
    }

    protected CloudResource executeOperationalRequest(CloudResource resource, ComputeRequest<Operation> request) throws IOException {
        try {
            Operation operation = request.execute();
            if (operation.getHttpErrorStatusCode() != null) {
                LOGGER.error("Error response in executing request, status {}, {}", operation.getHttpErrorStatusCode(), operation.getHttpErrorMessage());
                throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), resource.getName());
            }
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            GoogleJsonError jsonError = getGoogleJsonError(e);
            if (jsonError != null && HttpStatus.SC_CONFLICT == jsonError.getCode() && ResourceType.GCP_INSTANCE_GROUP.equals(resource.getType())) {
                LOGGER.warn("Resource {} already exists: {}", resource.getType(), resource.getName());
                return createOperationAwareCloudResource(resource, null);
            }
            throw exceptionHandlerWithThrow(e, resource.getName(), resourceType());
        }
    }
}
