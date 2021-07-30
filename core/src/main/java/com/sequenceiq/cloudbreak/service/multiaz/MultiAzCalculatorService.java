package com.sequenceiq.cloudbreak.service.multiaz;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class MultiAzCalculatorService {

    private static final String DEFAULT_RACK = "default-rack";

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzCalculatorService.class);

    @Inject
    private MultiAzValidator multiAzValidator;

    public String determineRackId(String subnetId, String availabilityZone) {
        return "/" +
                (isNullOrEmpty(availabilityZone) ?
                        (isNullOrEmpty(subnetId) ? DEFAULT_RACK : subnetId)
                        : availabilityZone);
    }

    public Map<String, String> prepareSubnetAzMap(DetailedEnvironmentResponse environment) {
        Map<String, String> subnetAzPairs = new HashMap<>();
        if (environment != null && environment.getNetwork() != null && environment.getNetwork().getSubnetMetas() != null) {
            for (Map.Entry<String, CloudSubnet> entry : environment.getNetwork().getSubnetMetas().entrySet()) {
                CloudSubnet value = entry.getValue();
                if (!isNullOrEmpty(value.getName())) {
                    subnetAzPairs.put(value.getName(), value.getAvailabilityZone());
                }
                if (!isNullOrEmpty(value.getId())) {
                    subnetAzPairs.put(value.getId(), value.getAvailabilityZone());
                }
            }
        }
        return subnetAzPairs;
    }

    public void calculateByRoundRobin(Map<String, String> subnetAzPairs, InstanceGroup instanceGroup) {
        Map<String, Integer> subnetUsage = new HashMap<>();
        Set<String> subnetIds = collectSubnetIds(instanceGroup);
        initializeSubnetUsage(subnetAzPairs, subnetIds, subnetUsage);
        collectCurrentSubnetUsage(instanceGroup, subnetUsage);

        if (!subnetIds.isEmpty() && multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                if (isNullOrEmpty(instanceMetaData.getSubnetId())) {
                    Integer numberOfInstanceInASubnet = searchTheSmallestInstanceCountForSubnets(subnetUsage);
                    String leastUsedSubnetId = searchTheSmallestUsedSubnetID(subnetUsage, numberOfInstanceInASubnet);

                    instanceMetaData.setSubnetId(leastUsedSubnetId);
                    instanceMetaData.setAvailabilityZone(subnetAzPairs.get(leastUsedSubnetId));

                    subnetUsage.put(leastUsedSubnetId, numberOfInstanceInASubnet + 1);
                }
            }
        }
    }

    public void calculateByRoundRobin(Map<String, String> subnetAzPairs, InstanceGroup instanceGroup, CloudInstance cloudInstance) {
        Map<String, Integer> subnetUsage = new HashMap<>();
        Set<String> subnetIds = collectSubnetIds(instanceGroup);
        initializeSubnetUsage(subnetAzPairs, subnetIds, subnetUsage);
        collectCurrentSubnetUsage(instanceGroup, subnetUsage);

        if (!subnetIds.isEmpty() && multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)) {
            if (isNullOrEmpty(cloudInstance.getSubnetId())) {
                Integer numberOfInstanceInASubnet = searchTheSmallestInstanceCountForSubnets(subnetUsage);
                String leastUsedSubnetId = searchTheSmallestUsedSubnetID(subnetUsage, numberOfInstanceInASubnet);

                cloudInstance.setSubnetId(leastUsedSubnetId);
                cloudInstance.setAvailabilityZone(subnetAzPairs.get(leastUsedSubnetId));
                subnetUsage.put(leastUsedSubnetId, numberOfInstanceInASubnet + 1);
            }
        }
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

    private void initializeSubnetUsage(Map<String, String> subnetAzPairs, Set<String> subnetIds, Map<String, Integer> subnetUsage) {
        for (String subnetId : subnetAzPairs.keySet()) {
            if (subnetIds.contains(subnetId)) {
                subnetUsage.put(subnetId, 0);
            }
        }
    }

    private void collectCurrentSubnetUsage(InstanceGroup instanceGroup, Map<String, Integer> subnetUsage) {

        for (InstanceMetaData instanceMetaData : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
            String subnetId = instanceMetaData.getSubnetId();
            if (!isNullOrEmpty(subnetId)) {
                Integer countOfInstances = subnetUsage.get(subnetId);
                if (countOfInstances != null) {
                    subnetUsage.put(subnetId, countOfInstances + 1);
                } else {
                    LOGGER.warn("Subnet ID {} is not present in the environment networks. Current usage: {}",
                            subnetId, subnetUsage.keySet());
                }
            }
        }
    }

    private Set<String> collectSubnetIds(InstanceGroup instanceGroup) {
        Set<String> allSubnetIds = new HashSet<>();
        InstanceGroupNetwork instanceGroupNetwork = instanceGroup.getInstanceGroupNetwork();
        if (instanceGroupNetwork != null) {
            Json attributes = instanceGroupNetwork.getAttributes();
            if (attributes != null) {
                List<String> subnetIds = (List<String>) attributes
                        .getMap()
                        .getOrDefault(SUBNET_IDS, new ArrayList<>());
                allSubnetIds.addAll(subnetIds);
            }

        }
        return allSubnetIds;
    }
}
