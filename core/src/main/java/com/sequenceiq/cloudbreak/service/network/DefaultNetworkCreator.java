package com.sequenceiq.cloudbreak.service.network;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static com.sequenceiq.cloudbreak.common.type.ResourceStatus.DEFAULT_DELETED;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.NetworkConfig;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.repository.NetworkRepository;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
public class DefaultNetworkCreator {

    private static final String DEFAULT_AWS_NETWORK_NAME = "default-aws-network";

    private static final String DEFAULT_GCP_NETWORK_NAME = "default-gcp-network";

    private static final String DEFAULT_AZURE_NETWORK_NAME = "default-azure-network";

    @Inject
    private NetworkRepository networkRepository;

    public void createDefaultNetworks(IdentityUser user) {
        Set<Network> defaultNetworks = networkRepository.findAllDefaultInAccount(user.getAccount());
        List<String> defaultNetworkNames = defaultNetworks.stream()
                .map(n -> n.getStatus() == DEFAULT_DELETED ? NameUtil.cutTimestampPostfix(n.getName()) : n.getName())
                .collect(Collectors.toList());
        createDefaultNetworkInstances(user, defaultNetworkNames);
    }

    private void createDefaultNetworkInstances(IdentityUser user, List<String> defaultNetworkNames) {
        if (!defaultNetworkNames.contains(DEFAULT_AWS_NETWORK_NAME)) {
            Network awsNetwork = new Network();
            setNetworkCommonFields(awsNetwork, DEFAULT_AWS_NETWORK_NAME, "Default network settings for AWS clusters.",
                    NetworkConfig.SUBNET_16, user, AWS);
            networkRepository.save(awsNetwork);
        }
        if (!defaultNetworkNames.contains(DEFAULT_AZURE_NETWORK_NAME)) {
            Network azureNetwork = new Network();
            setNetworkCommonFields(azureNetwork, DEFAULT_AZURE_NETWORK_NAME, "Default network settings for Azure clusters.",
                    NetworkConfig.SUBNET_16, user, CloudConstants.AZURE);
            networkRepository.save(azureNetwork);
        }
        if (!defaultNetworkNames.contains(DEFAULT_GCP_NETWORK_NAME)) {
            Network gcpNetwork = new Network();
            setNetworkCommonFields(gcpNetwork, DEFAULT_GCP_NETWORK_NAME, "Default network settings for Gcp clusters.",
                    NetworkConfig.SUBNET_16, user, GCP);
            networkRepository.save(gcpNetwork);
        }
    }

    private void setNetworkCommonFields(Network network, String name, String description, String subnet, IdentityUser user, String platform) {
        network.setName(name);
        network.setDescription(description);
        network.setSubnetCIDR(subnet);
        network.setOwner(user.getUserId());
        network.setAccount(user.getAccount());
        network.setStatus(ResourceStatus.DEFAULT);
        network.setPublicInAccount(true);
        network.setCloudPlatform(platform);
    }

}
