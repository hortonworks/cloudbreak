package com.sequenceiq.cloudbreak.service.network;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.NetworkConfig;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.repository.NetworkRepository;

@Service
public class DefaultNetworkCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultNetworkCreator.class);

    private static final String DEFAULT_AWS_NETWORK_NAME = "default-aws-network";
    private static final String DEFAULT_GCP_NETWORK_NAME = "default-gcp-network";

    @Inject
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

        Network awsNetwork = new Network();
        setNetworkCommonFields(awsNetwork, DEFAULT_AWS_NETWORK_NAME, "Default network settings for AWS clusters.",
                NetworkConfig.SUBNET_16, user, AWS);
        networks.add(networkRepository.save(awsNetwork));

        Network gcpNetwork = new Network();
        setNetworkCommonFields(gcpNetwork, DEFAULT_GCP_NETWORK_NAME, "Default network settings for Gcp clusters.",
                NetworkConfig.SUBNET_16, user, GCP);
        networks.add(networkRepository.save(gcpNetwork));

        return networks;
    }

    private void setNetworkCommonFields(Network network, String name, String description, String subnet, CbUser user, String platform) {
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
