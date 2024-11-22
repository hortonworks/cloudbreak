package com.sequenceiq.environment.network.service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.domain.ProvidedSubnetIds;

@Component
public class SubnetIdProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetIdProvider.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    public ProvidedSubnetIds subnets(NetworkDto network, Tunnel tunnel, CloudPlatform cloudPlatform, boolean multiAz) {
        LOGGER.debug("Choosing subnets, network: {},  platform: {}, tunnel: {}", network, cloudPlatform, tunnel);
        if (network == null || network.getSubnetIds() == null || network.getSubnetIds().isEmpty() || network.getCbSubnets() == null
                || network.getCbSubnets().isEmpty()) {
            LOGGER.debug("Check failed, returning null");
            return null;
        }
        NetworkConnector networkConnector = cloudPlatformConnectors
                .get(new CloudPlatformVariant(cloudPlatform.name(), cloudPlatform.name()))
                .networkConnector();
        if (networkConnector == null) {
            LOGGER.warn("Network connector is null for '{}' cloud platform, returning null", cloudPlatform.name());
            return null;
        }
        SubnetSelectionParameters subnetSelectionParameters = SubnetSelectionParameters
                .builder()
                .withHa(multiAz)
                .withTunnel(tunnel)
                .build();

        SubnetSelectionResult subnetSelectionResult = networkConnector
                .chooseSubnets(network.getCbSubnetValues(), subnetSelectionParameters);
        CloudSubnet selectedSubnet = subnetSelectionResult.hasResult()
                ? subnetSelectionResult.getResult().get(0)
                : fallback(network);
        LOGGER.info("Selected subnet as preferred: {}", selectedSubnet.getId());
        Set<CloudSubnet> selectedSubnets = subnetSelectionResult.hasResult()
                ? new HashSet<>(subnetSelectionResult.getResult())
                : fallbacks(network);

        return new ProvidedSubnetIds(
                selectedSubnet.getId(),
                selectedSubnets
                        .stream()
                        .map(CloudSubnet::getId)
                        .collect(Collectors.toSet()));
    }

    private CloudSubnet fallback(NetworkDto network) {
        CloudSubnet chosenSubnet = network.getSubnetMetas().values().iterator().next();
        LOGGER.debug("Choosing subnets, fallback strategy: '{}'", chosenSubnet.getId());
        return chosenSubnet;
    }

    private Set<CloudSubnet> fallbacks(NetworkDto network) {
        Set<CloudSubnet> chosenSubnets = new HashSet<>(network.getSubnetMetas().values());
        LOGGER.debug("Choosing subnets, fallback strategy: '{}'",
                chosenSubnets.stream().map(CloudSubnet::getId).collect(Collectors.toSet()));
        return chosenSubnets;
    }
}
