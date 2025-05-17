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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.AvailabilityZoneConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupAvailabilityZone;
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

    @Inject
    private AvailabilityZoneConverter availabilityZoneConverter;

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

    public void calculateByRoundRobin(Map<String, String> subnetAzPairs, InstanceGroup instanceGroup, Stack stack) {
        Map<String, Integer> subnetUsage = calculateCurrentSubnetUsage(subnetAzPairs, instanceGroup);
        if (!subnetUsage.isEmpty() && multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                updateSubnetIdForInstanceIfEmpty(subnetAzPairs, subnetUsage, instanceMetaData, stack);
            }
        } else {
            LOGGER.warn("Couldn't set subnetId for instances");
        }
    }

    public void updateSubnetIdForSingleInstanceIfEligible(Map<String, String> subnetAzPairs, Map<String, Integer> subnetUsage,
            InstanceMetaData instanceMetaData, InstanceGroup instanceGroup, Stack stack) {
        Map<String, Integer> filteredUsage = subnetUsage.entrySet().stream().filter(e -> subnetAzPairs.containsKey(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!filteredUsage.isEmpty() && multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)) {
            updateSubnetIdForInstanceIfEmpty(subnetAzPairs, filteredUsage, instanceMetaData, stack);
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

    private void updateSubnetIdForInstanceIfEmpty(Map<String, String> subnetAzPairs, Map<String, Integer> subnetUsage, InstanceMetaData instanceMetaData,
            Stack stack) {
        if (StringUtils.isBlank(instanceMetaData.getSubnetId())) {
            Integer numberOfInstanceInASubnet = searchTheSmallestInstanceCountForUsage(subnetUsage);
            String leastUsedSubnetId = searchTheSmallestUsedID(subnetUsage, numberOfInstanceInASubnet);
            LOGGER.debug("Least used subnet id [{}] with usage count [{}] is selected for instance [{}]",
                    leastUsedSubnetId, numberOfInstanceInASubnet, instanceMetaData.getInstanceId());

            instanceMetaData.setSubnetId(leastUsedSubnetId);
            if (isSubnetAzNeeded(stack)) {
                instanceMetaData.setAvailabilityZone(subnetAzPairs.get(leastUsedSubnetId));
            }

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
        Optional<Map.Entry<String, Integer>> usageFound = usage.entrySet()
                .stream()
                .filter(e -> e.getValue().equals(currentNumber))
                .findFirst();
        if (usageFound.isPresent()) {
            return usageFound.get().getKey();
        } else {
            throw new BadRequestException(String.format("Could not find least used subnet id", usage.keySet()));
        }
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
        if (stack.isMultiAz()) {
            if (instanceGroup.getInstanceGroupNetwork() == null) {
                instanceGroup.setInstanceGroupNetwork(new InstanceGroupNetwork());
            }
            Set<String> availabilityZonesForInstanceGroup = availabilityZoneConverter.getAvailabilityZonesFromJsonAttributes(
                    instanceGroup.getInstanceGroupNetwork().getAttributes());
            if (CollectionUtils.isEmpty(availabilityZonesForInstanceGroup)) {
                AvailabilityZoneConnector availabilityZoneConnector = getAvailabilityZoneConnector(stack);
                if (availabilityZoneConnector != null) {
                    Set<String> environmentZones = validateAndGetEnvironmentZones(environment, stack);
                    Set<String> availabilityZones = availabilityZoneConnector.getAvailabilityZones(getExtendedCloudCredential(stack), environmentZones,
                            instanceGroup.getTemplate().getInstanceType(), Region.region(environment.getLocation().getName()));
                    if (CollectionUtils.isEmpty(availabilityZones)) {
                        LOGGER.warn("There are no availability zones configured");
                        throw new BadRequestException(String.format("The %s region does not support Multi AZ configuration. " +
                                        "Please check %s " +
                                        "for more details. " +
                                        "It is also possible that the given %s instances on %s group are not supported in any specified %s zones.",
                                environment.getLocation().getName(),
                                getDocumentationLink(CloudPlatform.valueOf(stack.getCloudPlatform())),
                                instanceGroup.getTemplate().getInstanceType(),
                                instanceGroup.getGroupName(),
                                environmentZones.stream().sorted().collect(Collectors.toList())));
                    }
                    LOGGER.debug("Availability Zones for instance group are  {}", availabilityZones);
                    instanceGroup.getInstanceGroupNetwork().setAttributes(availabilityZoneConverter.getJsonAttributesWithAvailabilityZones(availabilityZones,
                            instanceGroup.getInstanceGroupNetwork().getAttributes()));
                    Set<InstanceGroupAvailabilityZone> instanceGroupAvailabilityZones = availabilityZones.stream().map(az -> {
                        InstanceGroupAvailabilityZone instanceGroupAvailabilityZone = new InstanceGroupAvailabilityZone();
                        instanceGroupAvailabilityZone.setInstanceGroup(instanceGroup);
                        instanceGroupAvailabilityZone.setAvailabilityZone(az);
                        return instanceGroupAvailabilityZone;
                    }).collect(Collectors.toSet());
                    instanceGroup.setAvailabilityZones(instanceGroupAvailabilityZones);
                } else {
                    LOGGER.debug("Implementation for AvailabilityZoneConnector is not present for CloudPlatform {} and PlatformVariant {}",
                            stack.getCloudPlatform(), stack.getPlatformvariant());
                }
            } else {
                LOGGER.debug("Availability Zones {} are already provided for Instance Group {} in the request",
                        String.join(",", availabilityZonesForInstanceGroup),
                        instanceGroup.getGroupName());
            }
        } else {
            LOGGER.debug("Multi AZ is not enabled for {}", stack.getName());
        }
    }

    public void populateAvailabilityZonesForInstances(Stack stack, InstanceGroup instanceGroup) {
        if (stack.isMultiAz()) {
            Set<String> availabilityZones = instanceGroup.getInstanceGroupNetwork() != null ? availabilityZoneConverter.getAvailabilityZonesFromJsonAttributes(
                    instanceGroup.getInstanceGroupNetwork().getAttributes()) : Collections.emptySet();
            if (!CollectionUtils.isEmpty(availabilityZones)) {
                Set<InstanceMetaData> instanceMetaDataSets = instanceGroup.getNotDeletedInstanceMetaDataSet();
                Map<String, Long> zoneToNodeCountMap = availabilityZones.stream().collect(Collectors.toMap(Function.identity(),
                        availabilityZone -> countInstancesForAvailabilityZone(instanceMetaDataSets, availabilityZone)));
                LOGGER.debug("Initialized zoneToNodeCountMap {}", zoneToNodeCountMap);
                for (InstanceMetaData instance : instanceMetaDataSets) {
                    if (instance.getAvailabilityZone() == null) {
                        String availabilityZone = Collections.min(zoneToNodeCountMap.entrySet(), Map.Entry.comparingByValue()).getKey();
                        zoneToNodeCountMap.put(availabilityZone, zoneToNodeCountMap.get(availabilityZone) + 1);
                        instance.setAvailabilityZone(availabilityZone);
                    }
                    LOGGER.debug("Availability Zones for instance {} is {}", instance.getInstanceId(), instance.getAvailabilityZone());
                }
            } else {
                LOGGER.debug("Availability Zones are not available for Instance Group {} ", instanceGroup.getGroupName());
            }
        } else {
            LOGGER.debug("Multi AZ is not enabled for {}", stack.getName());
        }
    }

    public AvailabilityZoneConnector getAvailabilityZoneConnector(Stack stack) {
        LOGGER.debug("CloudPlatform is {} PlatformVariant is {}", stack.getCloudPlatform(), stack.getPlatformvariant());
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(stack.getCloudPlatform()),
                Variant.variant(stack.getPlatformvariant()));
        return cloudPlatformConnectors.get(cloudPlatformVariant).availabilityZoneConnector();
    }

    private ExtendedCloudCredential getExtendedCloudCredential(Stack stack) {
        return extendedCloudCredentialConverter
                .convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn()));
    }

    private long countInstancesForAvailabilityZone(Set<InstanceMetaData> instanceMetaDataSets, String availabilityZone) {
        return instanceMetaDataSets.stream().filter(instance -> availabilityZone.equals(instance.getAvailabilityZone())).count();
    }

    private Set<String> validateAndGetEnvironmentZones(DetailedEnvironmentResponse detailedEnvironmentResponse, Stack stack) {
        Set<String> environmentZones;
        if (CloudPlatform.AWS.name().equals(stack.getCloudPlatform())) {
            environmentZones = detailedEnvironmentResponse.getNetwork().getSubnetMetas().values().stream()
                    .map(CloudSubnet::getAvailabilityZone)
                    .collect(Collectors.toSet());
        } else {
            environmentZones = detailedEnvironmentResponse.getNetwork().getAvailabilityZones(CloudPlatform.valueOf(stack.getCloudPlatform()));
        }
        if (CollectionUtils.isEmpty(environmentZones)) {
            LOGGER.error("Environment Zones are not configured");
            throw new BadRequestException(String.format("MultiAz is enabled but Availability Zones are not configured for environment %s." +
                    "Please modify the environment and configure Availability Zones", detailedEnvironmentResponse.getName()));
        }
        return environmentZones;
    }

    private String getDocumentationLink(CloudPlatform cloudPlatform) {
        switch (cloudPlatform) {
            case AZURE : return "https://learn.microsoft.com/en-us/azure/reliability/availability-zones-service-support";
            case GCP :  return "https://cloud.google.com/docs/geography-and-regions";
            default : return "";
        }
    }

    private boolean isSubnetAzNeeded(Stack stack) {
        return !stack.isMultiAz();
    }
}