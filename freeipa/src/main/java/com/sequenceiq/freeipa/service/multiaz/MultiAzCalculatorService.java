package com.sequenceiq.freeipa.service.multiaz;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.InstanceMetaData;

@Service
public class MultiAzCalculatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzCalculatorService.class);

    @Inject
    private MultiAzValidator multiAzValidator;

    public Map<String, String> prepareSubnetAzMap(DetailedEnvironmentResponse environment) {
        Map<String, String> subnetAzPairs = new HashMap<>();
        if (environment != null && environment.getNetwork() != null && environment.getNetwork().getSubnetMetas() != null) {
            for (Map.Entry<String, CloudSubnet> entry : environment.getNetwork().getSubnetMetas().entrySet()) {
                CloudSubnet value = entry.getValue();
                if (!Strings.isNullOrEmpty(value.getName())) {
                    subnetAzPairs.put(value.getName(), value.getAvailabilityZone());
                }
                if (!Strings.isNullOrEmpty(value.getId())) {
                    subnetAzPairs.put(value.getId(), value.getAvailabilityZone());
                }
            }
        }
        LOGGER.debug("Subnet-AZ map: {}", subnetAzPairs);
        return subnetAzPairs;
    }

    public void calculateByRoundRobin(Map<String, String> subnetAzPairs, InstanceGroup instanceGroup) {
        Map<String, Integer> subnetUsage = calculateCurrentSubnetUsage(subnetAzPairs, instanceGroup);
        if (!subnetUsage.isEmpty() && multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                updateSubnetIdForInstanceIfEmpty(subnetAzPairs, subnetUsage, instanceMetaData);
            }
        } else {
            LOGGER.warn("Couldn't set subnetId for instances");
        }
    }

    public void updateSubnetIdForSingleInstanceIfEligible(Map<String, String> subnetAzPairs, Map<String, Integer> subnetUsage,
            InstanceMetaData instanceMetaData, InstanceGroup instanceGroup) {
        if (!subnetUsage.isEmpty() && multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)) {
            updateSubnetIdForInstanceIfEmpty(subnetAzPairs, subnetUsage, instanceMetaData);
        }
    }

    private void updateSubnetIdForInstanceIfEmpty(Map<String, String> subnetAzPairs, Map<String, Integer> subnetUsage, InstanceMetaData instanceMetaData) {
        if (StringUtils.isBlank(instanceMetaData.getSubnetId())) {
            Integer numberOfInstanceInASubnet = searchTheSmallestInstanceCountForSubnets(subnetUsage);
            String leastUsedSubnetId = searchTheSmallestUsedSubnetID(subnetUsage, numberOfInstanceInASubnet);
            LOGGER.debug("Least used subnet id [{}] with usage count [{}] is selected for instance [{}]",
                    leastUsedSubnetId, numberOfInstanceInASubnet, instanceMetaData.getInstanceId());

            instanceMetaData.setSubnetId(leastUsedSubnetId);
            instanceMetaData.setAvailabilityZone(subnetAzPairs.get(leastUsedSubnetId));

            subnetUsage.put(leastUsedSubnetId, numberOfInstanceInASubnet + 1);
        }
    }

    public Map<String, Integer> calculateCurrentSubnetUsage(Map<String, String> subnetAzPairs, InstanceGroup instanceGroup) {
        Set<String> subnetIds = collectSubnetIds(instanceGroup);
        LOGGER.debug("Subnet ids used for availability zone selection: {}", subnetIds);
        Map<String, Integer> emptySubnetUsage = initializeSubnetUsage(subnetAzPairs, subnetIds);
        return collectCurrentSubnetUsage(instanceGroup, emptySubnetUsage);
    }

    private Integer searchTheSmallestInstanceCountForSubnets(Map<String, Integer> subnetUsage) {
        return Collections.min(subnetUsage.values());
    }

    private String searchTheSmallestUsedSubnetID(Map<String, Integer> subnetUsage, Integer currentNumber) {
        return subnetUsage.entrySet()
                .stream()
                .filter(e -> e.getValue().equals(currentNumber))
                .findFirst()
                .get()
                .getKey();
    }

    private Map<String, Integer> initializeSubnetUsage(Map<String, String> subnetAzPairs, Collection<String> subnetIds) {
        Map<String, Integer> emptySubentUsage = subnetAzPairs.keySet().stream()
                .filter(subnetIds::contains)
                .collect(Collectors.toMap(
                        subnetId -> subnetId,
                        subnetId -> 0
                ));
        LOGGER.debug("Empty subnet usage based on instancegroups and subnet-availabilityzone pairs: {}", emptySubentUsage);
        return emptySubentUsage;
    }

    private Map<String, Integer> collectCurrentSubnetUsage(InstanceGroup instanceGroup, Map<String, Integer> subnetUsage) {
        Map<String, Integer> subnetCurrentUsage = new HashMap<>(subnetUsage);
        instanceGroup.getNotDeletedInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getSubnetId)
                .filter(StringUtils::isNotBlank)
                .forEach(subnetId -> subnetCurrentUsage.computeIfPresent(subnetId, (key, currentCount) -> ++currentCount));
        LOGGER.debug("Current subnet usage: {}", subnetCurrentUsage);
        return subnetCurrentUsage;
    }

    private Set<String> collectSubnetIds(InstanceGroup instanceGroup) {
        InstanceGroupNetwork instanceGroupNetwork = instanceGroup.getInstanceGroupNetwork();
        if (instanceGroupNetwork != null && instanceGroupNetwork.getAttributes() != null) {
            Json attributes = instanceGroupNetwork.getAttributes();
            List<String> subnetIds = (List<String>) attributes
                    .getMap()
                    .getOrDefault(SUBNET_IDS, List.of());
            return new HashSet<>(subnetIds);
        } else {
            return Set.of();
        }
    }
}
