package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.common.api.type.ResourceType.GCP_NETWORK;
import static com.sequenceiq.common.api.type.ResourceType.GCP_SUBNET;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.DefaultNetworkConnector;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.network.GcpNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.network.GcpSubnetResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpSubnetSelectorService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class GcpNetworkConnector extends AbstractGcpResourceBuilder implements NetworkConnector, DefaultNetworkConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpNetworkConnector.class);

    @Value("${cb.gcp.subnet.ha.different.az.min:2}")
    private int minSubnetCountInDifferentAz;

    @Value("${cb.gcp.subnet.ha.different.az.max:3}")
    private int maxSubnetCountInDifferentAz;

    @Inject
    private GcpCloudSubnetProvider gcpCloudSubnetProvider;

    @Inject
    private GcpNetworkResourceBuilder gcpNetworkResourceBuilder;

    @Inject
    private GcpSubnetResourceBuilder gcpSubnetResourceBuilder;

    @Inject
    private GcpContextBuilder contextBuilders;

    @Inject
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Inject
    private ResourcePollTaskFactory statusCheckFactory;

    @Inject
    private GcpSubnetSelectorService gcpSubnetSelectorService;

    @Override
    public CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest networkCreationRequest) {
        CloudContext cloudContext = getCloudContext(networkCreationRequest);
        AuthenticatedContext auth = new AuthenticatedContext(cloudContext, networkCreationRequest.getCloudCredential());
        Network network = buildNetworkForCreation(networkCreationRequest);
        GcpContext context = contextBuilders.contextInit(cloudContext, auth, network, null, true);

        try {
            CloudResource networkResource = createNetwork(context, auth, network);
            List<CreatedSubnet> subnetList = getCloudSubNets(networkCreationRequest);
            for (CreatedSubnet createdSubnet : subnetList) {
                createSubnet(context, auth, buildSubnetForCreation(networkCreationRequest, createdSubnet.getCidr()), createdSubnet);
            }
            return new CreatedCloudNetwork(networkCreationRequest.getEnvName(), networkResource.getName(), getCreatedSubnets(subnetList));
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), GCP_NETWORK, networkCreationRequest.getEnvName());
        } catch (IOException e) {
            throw new GcpResourceException("Network creation failed due to IO exception", GCP_NETWORK, networkCreationRequest.getEnvName());
        }
    }

    @Override
    public void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest) {
        CloudContext cloudContext = getCloudContext(networkDeletionRequest);
        AuthenticatedContext auth = new AuthenticatedContext(cloudContext, networkDeletionRequest.getCloudCredential());
        Network network = buildNetworkForDeletion(networkDeletionRequest);
        GcpContext context = contextBuilders.contextInit(cloudContext, auth, network, null, true);

        try {
            for (String subnetId : networkDeletionRequest.getSubnetIds()) {
                deleteSubnet(context, auth, network, subnetId);
            }
            deleteNetwork(context, auth, network, networkDeletionRequest.getNetworkId());
        } catch (GoogleJsonResponseException e) {
            exceptionHandler(e, networkDeletionRequest.getStackName(), GCP_NETWORK);
        } catch (IOException e) {
            throw new GcpResourceException("Network deletion failed due to IO exception", GCP_NETWORK, networkDeletionRequest.getStackName());
        }
    }

    @Override
    public NetworkCidr getNetworkCidr(Network network, CloudCredential credential) {
        String subnetId = network.getStringParameter(GcpStackUtil.SUBNET_ID);
        String region = network.getStringParameter(GcpStackUtil.REGION);
        LOGGER.debug("Getting network cidrs for subnet {} in region {}", subnetId, region);
        Compute compute = GcpStackUtil.buildCompute(credential);
        String projectId = GcpStackUtil.getProjectId(credential);
        try {
            String ipCidrRange = compute.subnetworks().get(projectId, region, subnetId).execute().getIpCidrRange();
            return new NetworkCidr(ipCidrRange, Collections.singletonList(ipCidrRange));
        } catch (IOException e) {
            throw new GcpResourceException("Describe subnets failed due to IO exception", GCP_NETWORK, subnetId);
        }
    }

    @Override
    public SubnetSelectionResult filterSubnets(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        return gcpSubnetSelectorService.select(subnetMetas, subnetSelectionParameters);
    }

    @Override
    public int subnetCountInDifferentAzMin() {
        return minSubnetCountInDifferentAz;
    }

    @Override
    public int subnetCountInDifferentAzMax() {
        return maxSubnetCountInDifferentAz;
    }

    private CloudContext getCloudContext(NetworkCreationRequest networkRequest) {
        return new CloudContext(
                networkRequest.getEnvId(),
                networkRequest.getEnvName(),
                CloudPlatform.GCP.name(),
                CloudPlatform.GCP.name(),
                location(networkRequest.getRegion()),
                networkRequest.getCreatorCrn(),
                networkRequest.getAccountId());
    }

    private CloudContext getCloudContext(NetworkDeletionRequest networkRequest) {
        return new CloudContext(
                networkRequest.getEnvId(),
                networkRequest.getEnvName(),
                CloudPlatform.GCP.name(),
                CloudPlatform.GCP.name(),
                location(region(networkRequest.getRegion())),
                networkRequest.getUserId(),
                networkRequest.getAccountId());
    }

    private Network buildNetworkForCreation(NetworkCreationRequest networkRequest) {
        Subnet subnet = new Subnet(networkRequest.getNetworkCidr());
        Map<String, Object> params = new HashMap<>();
        params.put(GcpStackUtil.NETWORK_IP_RANGE, networkRequest.getNetworkCidr());
        return new Network(subnet, params);
    }

    private Network buildNetworkForDeletion(NetworkDeletionRequest networkRequest) {
        return new Network(null);
    }

    private Network buildSubnetForCreation(NetworkCreationRequest networkRequest, String cidr) {
        Network network = buildNetworkForCreation(networkRequest);
        Subnet subnet = new Subnet(cidr);
        network = new Network(subnet, network.getParameters());
        return network;
    }

    private CloudResource createSubnet(GcpContext context, AuthenticatedContext auth, Network network, CreatedSubnet subnet) {
        CloudResource cloudResource = gcpSubnetResourceBuilder.create(context, auth, network);
        try {
            cloudResource = gcpSubnetResourceBuilder.build(context, auth, network, null, cloudResource);
            PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(gcpSubnetResourceBuilder,
                    auth, Collections.singletonList(cloudResource), context, true);
            subnet.setSubnetId(cloudResource.getName());
            syncPollingScheduler.schedule(task);
        } catch (Exception e) {
            LOGGER.debug("Skipping resource creation: {}", e.getMessage());
        }
        return cloudResource;
    }

    private CloudResource createNetwork(GcpContext context, AuthenticatedContext auth, Network network) {
        CloudResource cloudResource = gcpNetworkResourceBuilder.create(context, auth, network);
        try {
            cloudResource = gcpNetworkResourceBuilder.build(context, auth, network, null, cloudResource);
            PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(gcpNetworkResourceBuilder,
                    auth, Collections.singletonList(cloudResource), context, true);
            syncPollingScheduler.schedule(task);
        } catch (Exception e) {
            LOGGER.debug("Skipping resource creation: {}", e.getMessage());
        }
        return cloudResource;
    }

    private void deleteNetwork(GcpContext context, AuthenticatedContext auth, Network network, String networkId) throws IOException {
        CloudResource networkResource = createNamedResource(GCP_NETWORK, networkId);
        try {
            CloudResource deletedResource = gcpNetworkResourceBuilder.delete(context, auth, networkResource, network);
            if (deletedResource != null) {
                PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(
                        gcpSubnetResourceBuilder, auth, Collections.singletonList(deletedResource), context, true);
                syncPollingScheduler.schedule(task);
            }
        } catch (Exception e) {
            LOGGER.debug("Skipping resource creation: {}", e.getMessage());
        }
    }

    private void deleteSubnet(GcpContext context, AuthenticatedContext auth, Network network, String subnetId) throws IOException {
        CloudResource subnetResource = createNamedResource(GCP_SUBNET, subnetId);
        try {
            CloudResource deletedResource = gcpSubnetResourceBuilder.delete(context, auth, subnetResource, network);
            if (deletedResource != null) {
                PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(
                        gcpSubnetResourceBuilder, auth, Collections.singletonList(deletedResource), context, true);
                syncPollingScheduler.schedule(task);
            }
        } catch (Exception e) {
            LOGGER.debug("Skipping resource creation: {}", e.getMessage());
        }
    }

    @Override
    public Platform platform() {
        return GcpConstants.GCP_PLATFORM;
    }

    @Override
    public Variant variant() {
        return GcpConstants.GCP_VARIANT;
    }

    private List<CreatedSubnet> getCloudSubNets(NetworkCreationRequest networkRequest) throws IOException {
        List<String> collect = networkRequest.getPublicSubnets().stream().map(e -> e.getCidr()).collect(Collectors.toList());
        collect.addAll(networkRequest.getPrivateSubnets().stream().map(e -> e.getCidr()).collect(Collectors.toList()));
        return gcpCloudSubnetProvider.provide(networkRequest, collect);
    }

    protected CloudResource createNamedResource(ResourceType type, String name) {
        return new CloudResource.Builder().type(type).name(name).build();
    }

    protected String checkException(GoogleJsonResponseException execute) {
        return execute.getDetails().getMessage();
    }

    private Set<CreatedSubnet> getCreatedSubnets(List<CreatedSubnet> createdSubnetList) {
        Set<CreatedSubnet> subnets = new HashSet<>();
        for (int i = 0; i < createdSubnetList.size(); i++) {
            CreatedSubnet createdSubnetIndexed = createdSubnetList.get(i);
            CreatedSubnet createdSubnet = new CreatedSubnet();
            createdSubnet.setSubnetId(createdSubnetIndexed.getSubnetId());
            createdSubnet.setCidr(createdSubnetList.get(i).getCidr());
            createdSubnet.setAvailabilityZone(createdSubnetList.get(i).getAvailabilityZone());
            createdSubnet.setMapPublicIpOnLaunch(true);
            createdSubnet.setIgwAvailable(true);
            subnets.add(createdSubnet);
        }
        return subnets;
    }
}
