package com.sequenceiq.cloudbreak.service.network;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.AwsNetwork;
import com.sequenceiq.cloudbreak.domain.AzureNetwork;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.GcpNetwork;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.NetworkStatus;
import com.sequenceiq.cloudbreak.repository.NetworkRepository;

@Service
public class DefaultNetworkCreator {
    private static final String DEFAULT_AWS_NETWORK_NAME = "default-aws-network";
    private static final String DEFAULT_AZURE_NETWORK_NAME = "default-azure-network";
    private static final String DEFAULT_GCP_NETWORK_NAME = "default-gcp-network";

    @Autowired
    private NetworkRepository networkRepository;

    public Set<Network> createDefaultNetworks(CbUser user) {
        Set<Network> networks = new HashSet<>();
        Set<Network> defaultNetworks = networkRepository.findAllDefaultInAccount(user.getAccount());

        if (defaultNetworks.isEmpty()) {
            networks = createDefaultNetworkInstances(user);
        }

        return networks;
    }

    private Set<Network> createDefaultNetworkInstances(CbUser user) {
        Set<Network> networks = new HashSet<>();

        AwsNetwork awsNetwork = new AwsNetwork();
        setNetworkCommonFields(awsNetwork, DEFAULT_AWS_NETWORK_NAME, "Default network settings for AWS clusters.", NetworkConfig.SUBNET_16, user);
        networks.add(networkRepository.save(awsNetwork));

        AzureNetwork azureNetwork = new AzureNetwork();
        setNetworkCommonFields(azureNetwork, DEFAULT_AZURE_NETWORK_NAME, "Default network settings for Azure clusters.", NetworkConfig.SUBNET_16, user);
        azureNetwork.setAddressPrefixCIDR(NetworkConfig.SUBNET_8);
        networks.add(networkRepository.save(azureNetwork));

        GcpNetwork gcpNetwork = new GcpNetwork();
        setNetworkCommonFields(gcpNetwork, DEFAULT_GCP_NETWORK_NAME, "Default network settings for Gcp clusters.", NetworkConfig.SUBNET_16, user);
        networks.add(networkRepository.save(gcpNetwork));

        return networks;
    }

    private void setNetworkCommonFields(Network network, String name, String description, String subnet, CbUser user) {
        network.setName(name);
        network.setDescription(description);
        network.setSubnetCIDR(subnet);
        network.setOwner(user.getUserId());
        network.setAccount(user.getAccount());
        network.setStatus(NetworkStatus.DEFAULT);
        network.setPublicInAccount(true);
    }

}
