package com.sequenceiq.cloudbreak.cloud;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkResourcesCreationRequest;

public interface DefaultNetworkConnector extends NetworkConnector {

    Logger LOGGER = LoggerFactory.getLogger(DefaultNetworkConnector.class);

    String NOT_ENOUGH_AZ = "Acceptable subnets are in %d different AZs, but subnets in %d different AZs required.";

    String NO_SUBNETS_FOR_REQUESTED_AZS = "There were no subnets provided in the required AZs: %s";

    @Override
    default SubnetSelectionResult chooseSubnets(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        LOGGER.debug("Trying to choose subnets from: {}.", subnetMetas);
        SubnetSelectionResult subnetSelectionResult;
        List<CloudSubnet> subnetsFilteredByRequestedAZ = filterSubnetsByRequiredAZs(subnetMetas, subnetSelectionParameters);
        if (subnetsFilteredByRequestedAZ.isEmpty()) {
            LOGGER.debug("Couldn't find subnets in the requested availability zones.");
            subnetSelectionResult = new SubnetSelectionResult(String.format(NO_SUBNETS_FOR_REQUESTED_AZS,
                subnetSelectionParameters.getAvailabilityZones()));
        } else {
            subnetSelectionResult = filterSubnets(subnetsFilteredByRequestedAZ, subnetSelectionParameters);
            if (subnetSelectionResult.hasResult()) {
                LOGGER.debug("There are subnets in the subnet selection: {}.", subnetSelectionResult);
                if (subnetSelectionParameters.isHa()) {
                    subnetSelectionResult = selectForHAScenario(subnetSelectionResult.getResult());
                } else {
                    subnetSelectionResult = selectForNonHAScenario(subnetSelectionResult.getResult());
                }
            }
            LOGGER.info("The subnet selection is: {}.", subnetSelectionResult);
        }
        return subnetSelectionResult;
    }

    private List<CloudSubnet> filterSubnetsByRequiredAZs(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        if (subnetMetas == null) {
            return List.of();
        }
        return subnetMetas.stream()
            .filter(subnetMeta -> subnetSelectionParameters.getAvailabilityZones() == null ||
                subnetSelectionParameters.getAvailabilityZones().isEmpty() ||
                subnetSelectionParameters.getAvailabilityZones().contains(subnetMeta.getAvailabilityZone()))
            .collect(Collectors.toList());
    }

    @Override
    default SubnetSelectionResult chooseSubnetsForPrivateEndpoint(Collection<CloudSubnet> subnetMetas, boolean existingNetwork) {
        return new SubnetSelectionResult(new ArrayList<>(subnetMetas));
    }

    @Override
    default void createProviderSpecificNetworkResources(NetworkResourcesCreationRequest networkResourcesCreationRequest) {
        throw new UnsupportedOperationException("No cloud provider specific resources exist on this cloud platform!");
    }

    SubnetSelectionResult filterSubnets(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters);

    int subnetCountInDifferentAzMin();

    int subnetCountInDifferentAzMax();

    default SubnetSelectionResult selectForHAScenario(List<CloudSubnet> subnets) {
        List<CloudSubnet> result = new ArrayList<>();
        Map<String, List<CloudSubnet>> groupedSubnetsByAz = groupSubnetsByAz(subnets);
        LOGGER.debug("The subnets groupped by AZs and the result is: {}.", groupedSubnetsByAz);
        // We dont have enough AZ
        if (!groupedSubnetsByAz.isEmpty() && !isDifferentAzCountEnough(groupedSubnetsByAz)) {
            LOGGER.debug("There is not enough different AZ in the subnet setup.");
            return new SubnetSelectionResult(String.format(NOT_ENOUGH_AZ, groupedSubnetsByAz.keySet().size(), subnetCountInDifferentAzMin()));
        }
        // We have enough AZ
        for (Map.Entry<String, List<CloudSubnet>> entry : groupedSubnetsByAz.entrySet()) {
            int random = new SecureRandom().nextInt(entry.getValue().size());
            CloudSubnet cloudSubnet = entry.getValue().get(random);
            LOGGER.debug("The selected subnet is: {} by random id {}.", cloudSubnet, random);
            result.add(cloudSubnet);
            if (result.size() == subnetCountInDifferentAzMax()) {
                LOGGER.debug("The selection logic already selected enough subnet which is {}.", result.size());
                break;
            }
        }
        // In case of mock
        if (groupedSubnetsByAz.isEmpty()) {
            result = subnets;
        }
        return new SubnetSelectionResult(result);
    }

    default SubnetSelectionResult selectForNonHAScenario(List<CloudSubnet> subnets) {
        List<CloudSubnet> result = new ArrayList<>();
        int random = new SecureRandom().nextInt(subnets.size());
        CloudSubnet cloudSubnet = subnets.get(random);
        LOGGER.info("The selected subnet is: {} by random id {}.", cloudSubnet, random);
        result.add(cloudSubnet);
        return new SubnetSelectionResult(result);
    }

    default boolean isDifferentAzCountEnough(Map<String, List<CloudSubnet>> groupedSubnetsByAz) {
        if (groupedSubnetsByAz.keySet().size() < subnetCountInDifferentAzMin()) {
            return false;
        }
        return true;
    }

    default Map<String, List<CloudSubnet>> groupSubnetsByAz(List<CloudSubnet> subnetMetas) {
        List<String> azs = new ArrayList<>();
        for (CloudSubnet subnetMeta : subnetMetas) {
            if (!azs.contains(subnetMeta.getAvailabilityZone()) && !Strings.isNullOrEmpty(subnetMeta.getAvailabilityZone())) {
                azs.add(subnetMeta.getAvailabilityZone());
            }
        }
        Map<String, List<CloudSubnet>> subnetsPerAz = new LinkedHashMap<>();
        // shuffle list to be more random
        Collections.shuffle(azs);
        // putting every az into the map
        if (!azs.isEmpty()) {
            for (String az : azs) {
                List<CloudSubnet> collect = subnetMetas
                        .stream()
                        .filter(e -> az.equals(e.getAvailabilityZone()))
                        .collect(Collectors.toList());
                subnetsPerAz.put(az, collect);
            }
        }
        return subnetsPerAz;
    }

}
