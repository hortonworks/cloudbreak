package com.sequenceiq.cloudbreak.cloud.gcp.network;

import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Address;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.domain.CloudRegion;
import com.sequenceiq.cloudbreak.domain.ResourceType;

@Service
public class GcpReservedIpResourceBuilder extends AbstractGcpNetworkBuilder {

    private static final int ORDER = 3;

    @Override
    public CloudResource create(GcpContext context, AuthenticatedContext auth, Network network, Security security) {
        String resourceName = getResourceNameService().resourceName(resourceType(), context.getName());
        return new CloudResource(resourceType(), resourceName);
    }

    @Override
    public CloudResource build(GcpContext context, AuthenticatedContext auth, Network network, Security security, CloudResource resource) throws Exception {
        String projectId = context.getProjectId();
        String region = CloudRegion.valueOf(context.getRegion()).region();

        Address address = new Address();
        address.setRegion(region);
        address.setName(resource.getName());

        Compute.Addresses.Insert networkInsert = context.getCompute().addresses().insert(projectId, region, address);
        try {
            Operation operation = networkInsert.execute();
            if (operation.getHttpErrorStatusCode() != null) {
                throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), resource.getName());
            }
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), resource.getName());
        }
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        Compute compute = context.getCompute();
        String projectId = context.getProjectId();
        String region = context.getRegion();
        try {
            Operation operation = compute.addresses().delete(projectId, CloudRegion.valueOf(region).region(), resource.getName()).execute();
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            exceptionHandler(e, resource.getName(), resourceType());
            return null;
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_RESERVED_IP;
    }

    @Override
    public int order() {
        return ORDER;
    }
}