package com.sequenceiq.freeipa.service.multiaz;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;

@Service
public class MultiAzCalculatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzCalculatorService.class);

    @Inject
    private MultiAzValidator multiAzValidator;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private CredentialService credentialService;

    public Map<String, String> prepareSubnetAzMap(DetailedEnvironmentResponse environment) {
        Map<String, String> subnetAzPairs = new HashMap<>();
        if (environment != null && environment.getNetwork() != null && environment.getNetwork().getSubnetMetas() != null) {
            for (Map.Entry<String, CloudSubnet> entry : environment.getNetwork().getSubnetMetas().entrySet()) {
                String subnetId = entry.getKey();
                CloudSubnet value = entry.getValue();
                if (value != null && value.getAvailabilityZone() != null) {
                    subnetAzPairs.put(subnetId, value.getAvailabilityZone());
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
        Map<String, Integer> filteredUsage = subnetUsage.entrySet().stream().filter(e -> subnetAzPairs.containsKey(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!filteredUsage.isEmpty() && multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)) {
            updateSubnetIdForInstanceIfEmpty(subnetAzPairs, filteredUsage, instanceMetaData);
        }
        subnetUsage.putAll(filteredUsage);
    }

    public Map<String, String> filterSubnetByLeastUsedAz(InstanceGroup instanceGroup, Map<String, String> subnetAzPairs) {
        Set<String> subnetIdsConfiguredOnInstanceGroup = collectSubnetIds(instanceGroup);
        Map<String, List<String>> azSubnetPairs = createAzSubnetPairsFromSubnetAzPairs(subnetAzPairs, subnetIdsConfiguredOnInstanceGroup);
        Map<String, Integer> azUsage = new HashMap<>();
        Set<String> azs = azSubnetPairs.keySet();
        azs.forEach(az -> azUsage.computeIfAbsent(az, k -> 0));
        collectCurrentAzUsage(instanceGroup, azUsage, subnetAzPairs);
        Integer numberOfInstanceInAnAz = searchTheSmallestInstanceCountForUsage(azUsage);
        String leastUsedAz = searchTheSmallestUsedID(azUsage, numberOfInstanceInAnAz);
        Set<String> subnetsForLeastUsedAz = new HashSet<>(azSubnetPairs.get(leastUsedAz));
        return subnetsForLeastUsedAz.stream().collect(Collectors.toMap(Function.identity(), v -> leastUsedAz));
    }

    private Map<String, List<String>> createAzSubnetPairsFromSubnetAzPairs(Map<String, String> subnetAzPairs, Set<String> subnetIdsConfiguredOnInstanceGroup) {
        Map<String, List<String>> ret = new HashMap<>();
        subnetAzPairs.forEach((subnet, az) -> {
            if (subnetIdsConfiguredOnInstanceGroup.contains(subnet)) {
                List<String> subnetIds = ret.computeIfAbsent(az, key -> new ArrayList<>());
                subnetIds.add(subnet);
            } else {
                LOGGER.debug("The subnet '{}' is part of the environment response but not configured for the instance group", subnet);
            }
        });
        return ret;
    }

    private void collectCurrentAzUsage(InstanceGroup instanceGroup, Map<String, Integer> azUsage, Map<String, String> subnetAzPairs) {
        for (InstanceMetaData instanceMetaData : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
            String subnetId = instanceMetaData.getSubnetId();
            if (!isNullOrEmpty(subnetId)) {
                String az = subnetAzPairs.get(subnetId);
                if (StringUtils.isNotEmpty(az)) {
                    Integer countOfInstances = azUsage.get(az);
                    if (countOfInstances != null) {
                        azUsage.put(az, countOfInstances + 1);
                    } else {
                        LOGGER.warn("AZ with subnet ID {} is not present in the environment networks. Current usage: {}",
                                subnetId, azUsage.keySet());
                    }
                } else {
                    LOGGER.debug("There is no availability zone data for subnet id: '{}', It is normal on Azure for now", subnetId);
                }
            } else {
                LOGGER.debug("Subnet id is null or empty for instance metadata: {}", instanceMetaData);
            }
        }
        LOGGER.debug("Current availability zone usage of instance group '{}': '{}'", instanceGroup.getGroupName(), azUsage);
    }

    private void updateSubnetIdForInstanceIfEmpty(Map<String, String> subnetAzPairs, Map<String, Integer> subnetUsage, InstanceMetaData instanceMetaData) {
        if (StringUtils.isBlank(instanceMetaData.getSubnetId())) {
            Integer numberOfInstanceInASubnet = searchTheSmallestInstanceCountForUsage(subnetUsage);
            String leastUsedSubnetId = searchTheSmallestUsedID(subnetUsage, numberOfInstanceInASubnet);
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

    private Integer searchTheSmallestInstanceCountForUsage(Map<String, Integer> usage) {
        return Collections.min(usage.values());
    }

    private String searchTheSmallestUsedID(Map<String, Integer> usage, Integer currentNumber) {
        return usage.entrySet()
                .stream()
                .filter(e -> e.getValue().equals(currentNumber))
                .findFirst()
                .get()
                .getKey();
    }

    private Map<String, Integer> initializeSubnetUsage(Map<String, String> subnetAzPairs, Collection<String> subnetIds) {
        Map<String, Integer> emptySubnetUsage = subnetAzPairs.keySet().stream()
                .filter(subnetIds::contains)
                .collect(Collectors.toMap(
                        subnetId -> subnetId,
                        subnetId -> 0
                ));
        LOGGER.debug("Empty subnet usage based on instancegroups and subnet-availabilityzone pairs: {}", emptySubnetUsage);
        return emptySubnetUsage;
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

    public void populateAvailabilityZones(Stack stack, DetailedEnvironmentResponse environment, InstanceGroup instanceGroup) {
        if (!stack.getMultiAz()) {
            return;
        }
        if (!CollectionUtils.isEmpty(instanceGroup.getInstanceGroupNetwork().getAvailabilityZones())) {
            return;
        }
        LOGGER.debug("CloudPlatform is {} PlatformVariant is {}", stack.getCloudPlatform(), stack.getPlatformvariant());
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(stack.getCloudPlatform()),
                Variant.variant(stack.getPlatformvariant()));
        AvailabilityZoneConnector availabilityZoneConnector = cloudPlatformConnectors.get(cloudPlatformVariant).availabilityZoneConnector();
        if (availabilityZoneConnector == null) {
            LOGGER.debug("Implementation for AvailabilityZoneConnector is not present for CloudPlatform {} and PlatformVariant {}",
                    stack.getCloudPlatform(), stack.getPlatformvariant());
            return;
        }
        Set<String> availabilityZones = availabilityZoneConnector.getAvailabilityZones(extendedCloudCredentialConverter
                .convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn())), environment.getNetwork()
                        .getAvailabilityZones(CloudPlatform.valueOf(stack.getCloudPlatform())),
                instanceGroup.getTemplate().getInstanceType(), Region.region(environment.getLocation().getName()));
        if (availabilityZones.size() < availabilityZoneConnector.getMinZonesForFreeIpa()) {
            LOGGER.error("Number of zones are less than min number of Zones");
            throw new BadRequestException(String.format("Based on configured availability zones and instance type, number of available zones " +
                    "for instance group %s are %s. Please configure at least %s zones for Multi Az deployment", instanceGroup.getGroupName(),
                    availabilityZones.size(), availabilityZoneConnector.getMinZonesForFreeIpa()));
        }
        LOGGER.debug("Availability Zones for instance group are  {}", availabilityZones);
        instanceGroup.getInstanceGroupNetwork().setAvailabilityZones(availabilityZones);
    }

    public void populateAvailabilityZonesForInstances(Stack stack, InstanceGroup instanceGroup) {
        if (!stack.getMultiAz()) {
            return;
        }
        Set<String> availabilityZones = instanceGroup.getInstanceGroupNetwork().getAvailabilityZones();
        if (CollectionUtils.isEmpty(availabilityZones)) {
            return;
        }
        Set<InstanceMetaData> instanceMetaDataSets = instanceGroup.getNotDeletedInstanceMetaDataSet();
        Map<String, Integer> zoneToNodeCountMap = availabilityZones.stream().collect(Collectors.toMap(Function.identity(), z -> 0));
        instanceMetaDataSets.stream().
                forEach(instance -> {
                    if (instance.getAvailabilityZone() == null) {
                        instance.setAvailabilityZone(Collections.min(zoneToNodeCountMap.entrySet(), Map.Entry.comparingByValue()).getKey());
                    }
                    if (zoneToNodeCountMap.containsKey(instance.getAvailabilityZone())) {
                        zoneToNodeCountMap.put(instance.getAvailabilityZone(), zoneToNodeCountMap.get(instance.getAvailabilityZone()) + 1);
                    } else {
                        LOGGER.warn("Instance {} has availability zone {} which is not assigned to subnet group {}", instance.getInstanceId(),
                                instance.getAvailabilityZone(), instanceGroup.getGroupName());
                    }

        });
    }
}
