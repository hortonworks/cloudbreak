package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Network;
import com.google.api.services.compute.model.NetworkList;
import com.google.api.services.compute.model.Subnetwork;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@Service
public class GcpPlatformResources implements PlatformResources {

    @Inject
    private GcpPlatformParameters gcpPlatformParameters;

    @Override
    public CloudNetworks networks(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        Compute compute = GcpStackUtil.buildCompute(cloudCredential);
        String projectId = GcpStackUtil.getProjectId(cloudCredential);
        Map<String, Set<CloudNetwork>> result = new HashMap<>();

        Set<CloudNetwork> cloudNetworks = new HashSet<>();
        if (compute != null) {
            NetworkList networkList = compute.networks().list(projectId).execute();
            List<Subnetwork> subnetworkList = compute.subnetworks().list(projectId, region.value()).execute().getItems();
            for (Network network : networkList.getItems()) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("gatewayIPv4", Strings.nullToEmpty(network.getGatewayIPv4()));
                properties.put("description", Strings.nullToEmpty(network.getDescription()));
                properties.put("IPv4Range", Strings.nullToEmpty(network.getIPv4Range()));
                properties.put("creationTimestamp", Strings.nullToEmpty(network.getCreationTimestamp()));

                Map<String, String> subnets = new HashMap<>();
                for (Subnetwork subnetwork : subnetworkList) {
                    if (network.getSubnetworks() != null
                            && network.getSubnetworks().contains(subnetwork.getSelfLink())) {
                        subnets.put(subnetwork.getName(), subnetwork.getName());
                    }
                }

                CloudNetwork cloudNetwork = new CloudNetwork(network.getName(), network.getId().toString(), subnets, properties);
                cloudNetworks.add(cloudNetwork);
            }

            for (Region actualRegion : gcpPlatformParameters.regions().types()) {
                if (regionMatch(actualRegion, region)) {
                    result.put(actualRegion.value(), cloudNetworks);
                }
            }
        }
        return new CloudNetworks(result);
    }

    @Override
    public CloudSshKeys sshKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudSshKeys();
    }

    @Override
    public CloudSecurityGroups securityGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudSecurityGroups();
    }
}
