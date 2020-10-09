package com.sequenceiq.cloudbreak.cloud.gcp.network;

import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getSharedProjectId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getSubnetId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.isExistingSubnet;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.isNewNetworkAndSubnet;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.isNewSubnetInExistingNetwork;
import static com.sequenceiq.common.api.type.ResourceType.GCP_SUBNET;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Subnetworks.Insert;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.Subnetwork;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpSubnetResourceBuilder extends AbstractGcpNetworkBuilder {

    public static final String SUBNET_NAME = "subnetName";

    @Override
    public CloudResource create(GcpContext context, AuthenticatedContext auth, Network network) {
        String resourceName = isExistingSubnet(network) ? getSubnetId(network) : getResourceNameService().resourceName(resourceType(), context.getName());
        return createNamedResource(resourceType(), resourceName);
    }

    @Override
    public CloudResource build(GcpContext context, AuthenticatedContext auth, Network network, Security security, CloudResource resource) throws Exception {
        if (isNewNetworkAndSubnet(network) || isNewSubnetInExistingNetwork(network)) {
            Compute compute = context.getCompute();
            String projectId = context.getProjectId();

            Subnetwork gcpSubnet = new Subnetwork();
            gcpSubnet.setName(resource.getName());
            gcpSubnet.setDescription(description());
            gcpSubnet.setIpCidrRange(network.getSubnet().getCidr());

            String networkName = context.getStringParameter(GcpNetworkResourceBuilder.NETWORK_NAME);
            if (isNotEmpty(getSharedProjectId(network))) {
                gcpSubnet.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s",
                        getSharedProjectId(network), networkName));
            } else {
                gcpSubnet.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", projectId, networkName));
            }

            Insert snInsert = compute.subnetworks().insert(projectId, auth.getCloudContext().getLocation().getRegion().value(), gcpSubnet);
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
        return new Builder().cloudResource(resource).persistent(false).build();
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception {
        if (isNewNetworkAndSubnet(network) || isNewSubnetInExistingNetwork(network)) {
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
