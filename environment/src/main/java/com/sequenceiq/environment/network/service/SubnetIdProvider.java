package com.sequenceiq.environment.network.service;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class SubnetIdProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetIdProvider.class);

    public String provide(NetworkDto network, Tunnel tunnel, CloudPlatform cloudPlatform) {
        LOGGER.debug("Choosing subnet, network: {},  platform: {}, tunnel: {}", network, cloudPlatform, tunnel);
        if (network == null || network.getSubnetIds() == null || network.getSubnetIds().isEmpty() || network.getSubnetMetas().isEmpty()) {
            LOGGER.debug("Check failed, returning null");
            return null;
        }
        return getSubnetIdByPreferedSubnetType(network, tunnel.useCcm(), cloudPlatform);
    }

    private String getSubnetIdByPreferedSubnetType(NetworkDto network, boolean preferPrivate, CloudPlatform cloudPlatform) {
        List<CloudSubnet> subnetMetas = new ArrayList<>(network.getSubnetMetas().values());
        String chosenSubnetId = AZURE.equals(cloudPlatform)
                ? subnetMetas.get(0).getId()
                : getSubnetIdByPreferedSubnetTypeAws(subnetMetas, preferPrivate);
        LOGGER.debug("Choosing subnet, selected id: {}", chosenSubnetId);
        return chosenSubnetId;
    }

    private String getSubnetIdByPreferedSubnetTypeAws(List<CloudSubnet> subnetMetas, boolean preferPrivate) {
        Optional<CloudSubnet> foundCloudSubnet = chooseSubnetPreferredStrategy(subnetMetas, preferPrivate);
        if (foundCloudSubnet.isEmpty()) {
            foundCloudSubnet = chooseSubnetAlternateStrategy(subnetMetas, preferPrivate);
            if (foundCloudSubnet.isEmpty()) {
                foundCloudSubnet = chooseSubnetFallbackStrategy(subnetMetas);
            }
        }
        return foundCloudSubnet.get().getId();
    }

    private Optional<CloudSubnet> chooseSubnetFallbackStrategy(List<CloudSubnet> subnetMetas) {
        LOGGER.debug("Choosing aws subnet, fallback to any subnet");
        return Optional.of(subnetMetas.get(0));
    }

    private Optional<CloudSubnet> chooseSubnetAlternateStrategy(List<CloudSubnet> subnetMetas, boolean preferPrivate) {
        LOGGER.debug("Choosing aws subnet, alternate strategy");
        return preferPrivate ? tryGetOnePublicSubnet(subnetMetas) : tryGetOnePrivateSubnet(subnetMetas);
    }

    private Optional<CloudSubnet> chooseSubnetPreferredStrategy(List<CloudSubnet> subnetMetas, boolean preferPrivate) {
        LOGGER.debug("Choosing aws subnet, preferring private: {}", preferPrivate);
        return preferPrivate ? tryGetOnePrivateSubnet(subnetMetas) : tryGetOnePublicSubnet(subnetMetas);
    }

    private Optional<CloudSubnet> tryGetOnePublicSubnet(List<CloudSubnet> subnetMetas) {
        return subnetMetas.stream()
                .filter(sn -> !sn.isPrivateSubnet() && sn.isMapPublicIpOnLaunch())
                .findAny();
    }

    private Optional<CloudSubnet> tryGetOnePrivateSubnet(List<CloudSubnet> subnetMetas) {
        return subnetMetas.stream()
                .filter(CloudSubnet::isPrivateSubnet)
                .findFirst();
    }

}
