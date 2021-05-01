package com.sequenceiq.cloudbreak.cloud.gcp.network;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Networks.Insert;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpNetworkResourceBuilder extends AbstractGcpNetworkBuilder {

    public static final String NETWORK_NAME = "netName";

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Override
    public CloudResource create(GcpContext context, AuthenticatedContext auth, Network network) {
        String name = gcpStackUtil.isExistingNetwork(network) ?
                gcpStackUtil.getCustomNetworkId(network) :
                getResourceNameService().resourceName(resourceType(), context.getName());
        return createNamedResource(resourceType(), name);
    }

    @Override
    public CloudResource build(GcpContext context, AuthenticatedContext auth, Network network, Security security,
        CloudResource resource) throws Exception {
        if (!gcpStackUtil.isExistingNetwork(network)) {
            Compute compute = context.getCompute();
            String projectId = context.getProjectId();

            com.google.api.services.compute.model.Network gcpNetwork = new com.google.api.services.compute.model.Network();
            gcpNetwork.setName(resource.getName());
            gcpNetwork.setDescription(description());
            gcpNetwork.setAutoCreateSubnetworks(false);
            Insert networkInsert = compute.networks().insert(projectId, gcpNetwork);
            try {
                Operation operation = networkInsert.execute();
                if (operation.getHttpErrorStatusCode() != null) {
                    throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), resource.getName());
                }
                context.putParameter(NETWORK_NAME, resource.getName());
                return createOperationAwareCloudResource(resource, operation);
            } catch (GoogleJsonResponseException e) {
                throw new GcpResourceException(checkException(e), resourceType(), resource.getName());
            }
        }
        context.putParameter(NETWORK_NAME, resource.getName());
        return new Builder().cloudResource(resource).persistent(false).build();
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception {
        if (!gcpStackUtil.isExistingNetwork(network)) {
            Compute compute = context.getCompute();
            String projectId = context.getProjectId();
            try {
                Operation operation = compute.networks().delete(projectId, resource.getName()).execute();
                return createOperationAwareCloudResource(resource, operation);
            } catch (GoogleJsonResponseException e) {
                exceptionHandler(e, resource.getName(), resourceType());
                return null;
            }
        }
        return null;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_NETWORK;
    }

    @Override
    public int order() {
        return 0;
    }
}
