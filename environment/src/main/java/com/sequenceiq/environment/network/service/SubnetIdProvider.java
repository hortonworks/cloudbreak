package com.sequenceiq.environment.network.service;

import java.util.ArrayList;
import java.util.Random;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class SubnetIdProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetIdProvider.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    public String provide(NetworkDto network, Tunnel tunnel, CloudPlatform cloudPlatform) {
        LOGGER.debug("Choosing subnet, network: {},  platform: {}, tunnel: {}", network, cloudPlatform, tunnel);
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

        SubnetSelectionResult subnetSelectionResult = networkConnector
                .selectSubnets(new ArrayList<>(network.getCbSubnets().values()), SubnetSelectionParameters.builder().withTunnel(tunnel).build());
        CloudSubnet selectedSubnet = subnetSelectionResult.hasResult()
                ? subnetSelectionResult.getResult().get(new Random().nextInt(subnetSelectionResult.getResult().size()))
                : fallback(network);
        return selectedSubnet.getId();
    }

    private CloudSubnet fallback(NetworkDto network) {
        CloudSubnet chosenSubnet = network.getSubnetMetas().values().iterator().next();
        LOGGER.debug("Choosing subnet, fallback strategy: '{}'", chosenSubnet.getId());
        return chosenSubnet;
    }
}
