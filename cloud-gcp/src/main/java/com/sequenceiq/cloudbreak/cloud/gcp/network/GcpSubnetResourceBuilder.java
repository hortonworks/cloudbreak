package com.sequenceiq.cloudbreak.cloud.gcp.network;

import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getSubnetId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.isExistingSubnet;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.legacyNetwork;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.newNetworkAndSubnet;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.newSubnetInExistingNetwork;
import static com.sequenceiq.cloudbreak.common.type.ResourceType.GCP_SUBNET;

import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.Subnetwork;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.template.ResourceNotNeededException;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class GcpSubnetResourceBuilder extends AbstractGcpNetworkBuilder {

    public static final String SUBNET_NAME = "subnetName";

    @Override
    public CloudResource create(GcpContext context, AuthenticatedContext auth, Network network) {
        if (legacyNetwork(network)) {
            throw new ResourceNotNeededException("Legacy GCP networks doesn't support subnets. Subnet won't be created.");
        }
        String resourceName = isExistingSubnet(network) ? getSubnetId(network) : getResourceNameService().resourceName(resourceType(), context.getName());
        return createNamedResource(resourceType(), resourceName);
    }

    @Override
    public CloudResource build(GcpContext context, AuthenticatedContext auth, Network network, Security security, CloudResource resource) throws Exception {
        if (newNetworkAndSubnet(network) || newSubnetInExistingNetwork(network)) {
            Compute compute = context.getCompute();
            String projectId = context.getProjectId();

            Subnetwork gcpSubnet = new Subnetwork();
            gcpSubnet.setName(resource.getName());
            gcpSubnet.setIpCidrRange(network.getSubnet().getCidr());

            String networkName = context.getStringParameter(GcpNetworkResourceBuilder.NETWORK_NAME);
            gcpSubnet.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", projectId, networkName));

            Compute.Subnetworks.Insert snInsert = compute.subnetworks().insert(projectId, auth.getCloudContext().getLocation().getRegion().value(), gcpSubnet);
            try {
                Operation operation = snInsert.execute();
                if (operation.getHttpErrorStatusCode() != null) {
                    throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), resource.getName());
                }
                context.putParameter(SUBNET_NAME, resource.getName());
                return createOperationAwareCloudResource(resource, operation);
            } catch (GoogleJsonResponseException e) {
                throw new GcpResourceException(checkException(e), resourceType(), resource.getName());
            }
        }
        context.putParameter(SUBNET_NAME, resource.getName());
        return new CloudResource.Builder().cloudResource(resource).persistent(false).build();
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception {
        if (newNetworkAndSubnet(network) || newSubnetInExistingNetwork(network)) {
            Compute compute = context.getCompute();
            String projectId = context.getProjectId();
            try {
                Operation operation = compute.subnetworks().delete(projectId, context.getLocation().getRegion().value(), resource.getName()).execute();
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
        return GCP_SUBNET;
    }

    @Override
    public int order() {
        return 1;
    }
}
