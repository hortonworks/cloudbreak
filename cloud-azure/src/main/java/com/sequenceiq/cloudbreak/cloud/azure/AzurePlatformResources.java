package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.Subnet;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@Service
public class AzurePlatformResources implements PlatformResources {

    @Inject
    private AzureClientService azureClientService;

    @Override
    public CloudNetworks networks(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        AzureClient client = azureClientService.getClient(cloudCredential);
        Map<String, Set<CloudNetwork>> result = new HashMap<>();

        for (Network network : client.getNetworks().list()) {
            String actualRegion = network.region().label();
            if (regionMatch(actualRegion, region)) {
                Set<String> subnets = new HashSet<>();
                for (Entry<String, Subnet> subnet : network.subnets().entrySet()) {
                    subnets.add(subnet.getValue().name());
                }
                Map<Object, Object> properties = new HashMap<>();
                properties.put("addressSpaces", network.addressSpaces());
                properties.put("dnsServerIPs", network.dnsServerIPs());
                properties.put("resourceGroupName", network.resourceGroupName());
                CloudNetwork cloudNetwork = new CloudNetwork(network.name(), subnets, new HashMap<>());
                if (result.get(actualRegion) == null) {
                    result.put(actualRegion, new HashSet<>());
                }
                result.get(actualRegion).add(cloudNetwork);
            }
        }
        return new CloudNetworks(result);
    }

    @Override
    public CloudSshKeys sshKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudSshKeys();
    }

    @Override
    public CloudSecurityGroups securityGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        AzureClient client = azureClientService.getClient(cloudCredential);
        Map<String, Set<CloudSecurityGroup>> result = new HashMap<>();

        for (NetworkSecurityGroup securityGroup : client.getSecurityGroups().list()) {
            String actualRegion = securityGroup.region().label();
            if (regionMatch(actualRegion, region)) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("resourceGroupName", securityGroup.resourceGroupName());
                properties.put("networkInterfaceIds", securityGroup.networkInterfaceIds());
                CloudSecurityGroup cloudSecurityGroup = new CloudSecurityGroup(securityGroup.name(), securityGroup.key(), properties);
                if (result.get(actualRegion) == null) {
                    result.put(actualRegion, new HashSet<>());
                }
                result.get(actualRegion).add(cloudSecurityGroup);
            }
        }
        return new CloudSecurityGroups(result);
    }
}
