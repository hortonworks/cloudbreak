package com.sequenceiq.cloudbreak.service.multiaz;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.cluster.InstanceGroupSubnetCollector;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

public class InstanceMetadataAvailabilityZoneCalculator {

    public static final Set<String> ZONAL_SUBNET_CLOUD_PLATFORMS = Set.of(CloudPlatform.AWS.name());

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetadataAvailabilityZoneCalculator.class);

    private static final String GROUP_NOT_EXISTS_WITH_NAME_PATTERN = "Invalid state: instance group with name '%s' has been requested to be scaled, "
            + "but the cluster doesn't contain group with that name.";

    private static final String DEFAULT_RACK = "default-rack";

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private InstanceGroupSubnetCollector instanceGroupSubnetCollector;

    private static InstanceGroupDto getInstanceGroupDtoByGroupName(StackDtoDelegate stack, String hostGroupName) {
        return stack.getInstanceGroupDtos().stream()
                .filter(ig -> hostGroupName.equals(ig.getInstanceGroup().getGroupName()))
                .findFirst()
                .orElseThrow(() -> new CloudbreakServiceException(String.format(GROUP_NOT_EXISTS_WITH_NAME_PATTERN, hostGroupName)));
    }

    private static Set<InstanceMetaData> getInstancesByInstanceGroupId(Set<InstanceMetaData> notDeletedInstanceMetadataForStack, Long instanceGroupId) {
        return notDeletedInstanceMetadataForStack.stream()
                .filter(im -> instanceGroupId.equals(im.getInstanceGroupId()))
                .collect(Collectors.toSet());
    }

    public void populate(Long stackId) {
        Stack stack = stackService.getByIdWithLists(stackId);
        populate(stack);
    }

    public boolean populateForScaling(StackDtoDelegate stack, Set<String> hostGroupsWithInstancesToCreate,
            boolean repair, NetworkScaleDetails networkScaleDetails) {
        boolean updateHappened = Boolean.FALSE;
        if (populateSupportedOnStack(stack)) {
            Set<InstanceMetaData> updatedInstances = new HashSet<>();
            Set<InstanceMetaData> notDeletedInstances = instanceMetaDataService.getNotDeletedInstanceMetadataWithNetworkByStackId(stack.getId());
            for (String hostGroupName : hostGroupsWithInstancesToCreate) {
                populateOnGroupForScalingAndRepair(stack, repair, hostGroupName, notDeletedInstances, updatedInstances, networkScaleDetails);
            }
            updateInstancesMetaData(updatedInstances);
            if (CollectionUtils.isNotEmpty(updatedInstances)) {
                updateHappened = Boolean.TRUE;
            }
        } else {
            LOGGER.debug("Stack is NOT multi-AZ enabled or AvailabilityZone connector doesn't exist for platform , no need to populate zones for instances");
        }
        return updateHappened;
    }

    protected boolean populateSupportedOnStack(Stack stack) {
        return stack.isMultiAz() && availabilityZoneConnectorExistsForPlatform(stack.getCloudPlatform(), stack.getPlatformVariant());
    }

    protected boolean populateSupportedOnStack(StackDtoDelegate stack) {
        return stack.getStack().isMultiAz() && availabilityZoneConnectorExistsForPlatform(stack.getCloudPlatform(), stack.getPlatformVariant());
    }

    protected void populate(Stack stack) {
        if (populateSupportedOnStack(stack)) {
            LOGGER.debug("Starting to calculate availability zones for instances of stack.");
            String stackLevelSubnetId = getStackSubnetIdIfExists(stack);
            Set<InstanceMetaData> instancesToBeUpdated = new HashSet<>();
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                instancesToBeUpdated.addAll(populateAvailabilityZonesOnGroup(instanceGroup, stackLevelSubnetId, stack.getNetwork()));
            }
            updateInstancesMetaData(instancesToBeUpdated);
        } else {
            LOGGER.debug("Stack is NOT multi-AZ enabled or AvailabilityZone connector doesn't exist for platform , no need to populate zones for instances");
        }
    }

    protected Set<InstanceMetaData> populateAvailabilityZonesOnGroup(InstanceGroup instanceGroup, String stackLevelSubnetId, Network network) {
        String groupName = instanceGroup.getGroupName();
        Set<String> availabilityZonesForGroup = instanceGroup.getAvailabilityZones();
        if (CollectionUtils.isNotEmpty(availabilityZonesForGroup)) {
            LOGGER.info("Availability zones to distribute the instances over: '{}' for group: '{}'", availabilityZonesForGroup, groupName);
            Set<InstanceMetaData> instancesInGroup = instanceGroup.getNotDeletedInstanceMetaDataSet();
            return populateAvailabilityZoneOfInstances(availabilityZonesForGroup, instancesInGroup, instanceGroup, stackLevelSubnetId, network);
        } else {
            String msg = String.format("Multi-AZ is enabled for the stack, but no availability zone is available for the group: '%s'", groupName);
            LOGGER.warn(msg);
            throw new CloudbreakServiceException(msg);
        }
    }

    protected Set<InstanceMetaData> populateAvailabilityZoneOfInstances(Set<String> availabilityZonesForGroup, Set<InstanceMetaData> instanceMetaDataSet,
            InstanceGroup instanceGroup, String stackLevelSubnetId, Network network) {
        return populateAvailabilityZoneOfInstances(availabilityZonesForGroup, instanceMetaDataSet, instanceGroup, stackLevelSubnetId, network, Set.of());
    }

    private Set<InstanceMetaData> populateAvailabilityZoneOfInstances(Set<String> availabilityZonesForGroup, Set<InstanceMetaData> instanceMetaDataSet,
                InstanceGroup instanceGroup, String stackLevelSubnetId, Network network, Set<String> preferredSubnetIds) {
        Set<InstanceMetaData> updatedInstances = new HashSet<>();
        String groupName = instanceGroup.getGroupName();
        Map<String, Long> zoneToNodeCountMap = getAvailabilityZoneNodeCountMap(availabilityZonesForGroup, instanceMetaDataSet);
        LOGGER.debug("Initialized zoneToNodeCountMap {} for group '{}'", zoneToNodeCountMap, groupName);
        Map<String, Map<String, Long>> subnetUsageGroupedByAzMap = new HashMap<>();
        Optional<String> optCloudPlatform = Optional.of(instanceGroup)
                .map(InstanceGroup::getInstanceGroupNetwork)
                .map(InstanceGroupNetwork::getCloudPlatform);
        if (optCloudPlatform.isPresent() && ZONAL_SUBNET_CLOUD_PLATFORMS.contains(optCloudPlatform.get())) {
            Map<String, String> subnetAzMap = getSubnetAzMapFromEnvironment(instanceGroup, network);
            populateSubnetUsageGroupedByAzMap(subnetUsageGroupedByAzMap, subnetAzMap);
        }
        for (InstanceMetaData instance : instanceMetaDataSet) {
            if (StringUtils.isEmpty(instance.getAvailabilityZone())) {
                String availabilityZone = getAndIncreaseAvailabilityZoneUsage(zoneToNodeCountMap);
                instance.setAvailabilityZone(availabilityZone);

                if (!subnetUsageGroupedByAzMap.isEmpty()) {
                    setSubnetIdAndIncreaseUsageCount(preferredSubnetIds, instance, subnetUsageGroupedByAzMap);
                } else if (StringUtils.isEmpty(instance.getSubnetId())) {
                    instance.setSubnetId(stackLevelSubnetId);
                }
                setInstanceRackIdAndLogUpdateDetails(instance, availabilityZone, groupName);
                updatedInstances.add(instance);
            }
        }
        return updatedInstances;
    }

    private void setSubnetIdAndIncreaseUsageCount(Set<String> preferredSubnetIds, InstanceMetaData instance,
            Map<String, Map<String, Long>> subnetUsageGroupedByAzMap) {
        String subnetId = getLeastUsedSubnetIdInAvailabilityZone(subnetUsageGroupedByAzMap, instance.getAvailabilityZone(), preferredSubnetIds);
        instance.setSubnetId(subnetId);
        Long existingInstanceCountInSubnet = subnetUsageGroupedByAzMap.get(instance.getAvailabilityZone()).getOrDefault(subnetId, 0L);
        subnetUsageGroupedByAzMap.get(instance.getAvailabilityZone()).put(subnetId, existingInstanceCountInSubnet + 1);
        LOGGER.debug("Subnet usage has been incremented to {} for subnet '{}' and AZ '{}'", existingInstanceCountInSubnet + 1,
                subnetId, instance.getAvailabilityZone());
    }

    private void setInstanceRackIdAndLogUpdateDetails(InstanceMetaData instance, String availabilityZone, String groupName) {
        String previousRackId = instance.getRackId();
        instance.setRackId("/" +
                (isNullOrEmpty(availabilityZone) ?
                        (isNullOrEmpty(instance.getSubnetId()) ? DEFAULT_RACK : instance.getSubnetId())
                        : availabilityZone));
        String privateIdWithGroupName = String.format("group: %s, private id: %s", groupName, instance.getPrivateId());
        String instanceName = StringUtils.isNotEmpty(instance.getInstanceId()) ? instance.getInstanceId() : privateIdWithGroupName;
        LOGGER.info("Set availability zone('{}') and subnet('{}') for instance '{}'", availabilityZone, instance.getSubnetId(), instanceName);
        LOGGER.info("Rack Id updated from {} to {} for instance '{}'", previousRackId, instance.getRackId(), instanceName);
    }

    protected String getStackSubnetIdIfExists(StackDtoDelegate stack) {
        return Optional.ofNullable(stack.getNetwork())
                .map(Network::getAttributes)
                .map(Json::getMap)
                .map(attr -> attr.get(SUBNET_ID))
                .map(Object::toString)
                .orElse(null);
    }

    private Map<String, String> getSubnetAzMapFromEnvironment(InstanceGroup instanceGroup, Network network) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse(instanceGroup.getStack().getEnvironmentCrn());
        Map<String, CloudSubnet> subnetMetas = detailedEnvironmentResponse.getNetwork().getSubnetMetas();
        Map<String, String> subnetAzMap = instanceGroupSubnetCollector.collect(instanceGroup, network)
                .stream()
                .collect(toMap(subnetId -> subnetId, subnetId -> subnetMetas.get(subnetId).getAvailabilityZone()));
        return subnetAzMap;
    }

    private void populateSubnetUsageGroupedByAzMap(Map<String, Map<String, Long>> subnetUsageGroupedByAzMap, Map<String, String> subnetAzMap) {
        for (Map.Entry<String, String> entry : subnetAzMap.entrySet()) {
            String subnet = entry.getKey();
            String availabilityZone = entry.getValue();
            subnetUsageGroupedByAzMap.putIfAbsent(availabilityZone, new HashMap<>());
            subnetUsageGroupedByAzMap.get(availabilityZone).put(subnet, 0L);
            LOGGER.debug("SubnetId '{}' added to AZ '{}' in subnetUsageMap", subnet, availabilityZone);
        }
    }

    private String getLeastUsedSubnetIdInAvailabilityZone(Map<String, Map<String, Long>> subnetUsageGroupedByAzMap, String availabilityZone,
            Set<String> preferredSubnetIds) {
        Map<String, Long> subnetIdUsageInAzMap = subnetUsageGroupedByAzMap.get(availabilityZone);
        if (CollectionUtils.isNotEmpty(preferredSubnetIds)) {
            subnetIdUsageInAzMap = subnetIdUsageInAzMap.entrySet()
                    .stream()
                    .filter(subnetIdUsage -> preferredSubnetIds.contains(subnetIdUsage.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return Collections.min(subnetIdUsageInAzMap.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private Map<String, Long> getAvailabilityZoneNodeCountMap(Set<String> availabilityZones, Set<InstanceMetaData> instances) {
        return availabilityZones.stream().collect(toMap(Function.identity(),
                availabilityZone -> countInstancesForAvailabilityZone(instances, availabilityZone)));
    }

    private String getAndIncreaseAvailabilityZoneUsage(Map<String, Long> zoneToNodeCountMap) {
        String availabilityZone = Collections.min(zoneToNodeCountMap.entrySet(), Map.Entry.comparingByValue()).getKey();
        zoneToNodeCountMap.put(availabilityZone, zoneToNodeCountMap.get(availabilityZone) + 1);
        return availabilityZone;
    }

    protected void updateInstancesMetaData(Set<InstanceMetaData> instancesToBeUpdated) {
        if (CollectionUtils.isNotEmpty(instancesToBeUpdated)) {
            LOGGER.debug("Saving instance meta data set with populated availability zones.");
            instanceMetaDataService.saveAll(instancesToBeUpdated);
        }
    }

    protected StackService getStackService() {
        return stackService;
    }

    protected DetailedEnvironmentResponse getDetailedEnvironmentResponse(String environmentCrn) {
        return environmentClientService.getByCrnAsInternal(environmentCrn);
    }

    private boolean availabilityZoneConnectorExistsForPlatform(String cloudPlatform, String platformVariant) {
        LOGGER.debug("CloudPlatform is {} PlatformVariant is {}", cloudPlatform, platformVariant);
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(cloudPlatform),
                Variant.variant(platformVariant));
        return Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant).availabilityZoneConnector()).isPresent();
    }

    private long countInstancesForAvailabilityZone(Set<InstanceMetaData> instanceMetaDataSet, String availabilityZone) {
        return instanceMetaDataSet.stream().filter(instance -> availabilityZone.equals(instance.getAvailabilityZone())).count();
    }

    private void populateOnGroupForScalingAndRepair(StackDtoDelegate stack, boolean repair, String hostGroupName, Set<InstanceMetaData> notDeletedInstances,
            Set<InstanceMetaData> updatedInstances, NetworkScaleDetails networkScaleDetails) {
        LOGGER.debug("Populating availability zone for instances in group: '{}'", hostGroupName);
        InstanceGroupDto instanceGroupDto = getInstanceGroupDtoByGroupName(stack, hostGroupName);
        Long instanceGroupId = instanceGroupDto.getInstanceGroup().getId();
        Set<InstanceMetaData> notDeletedInstancesForGroup = getInstancesByInstanceGroupId(notDeletedInstances, instanceGroupId);
        if (repair) {
            updatedInstances.addAll(populateOnInstancesOfGroupForRepair(stack, hostGroupName, notDeletedInstancesForGroup, networkScaleDetails));
        } else {
            updatedInstances.addAll(populateOnInstancesOfGroupForScaling(stack, hostGroupName, notDeletedInstancesForGroup, networkScaleDetails));
        }
    }

    private Set<InstanceMetaData> populateOnInstancesOfGroupForRepair(StackDtoDelegate stack, String hostGroupName,
            Set<InstanceMetaData> notDeletedInstancesForGroup, NetworkScaleDetails networkScaleDetails) {
        LOGGER.info("Populating availability zones of instances for repair in group: '{}'", hostGroupName);
        Set<InstanceMetaData> updatedInstances = new HashSet<>();
        StackView stackView = stack.getStack();
        String stackLevelSubnetId = getStackSubnetIdIfExists(stack);
        Set<InstanceMetaData> instancesToUpdate = notDeletedInstancesForGroup.stream()
                .filter(im -> InstanceStatus.REQUESTED.equals(im.getInstanceStatus()) && StringUtils.isEmpty(im.getAvailabilityZone()))
                .collect(Collectors.toSet());
        boolean hasInstanceWithoutDiskWithAZ = false;
        for (InstanceMetaData im : instancesToUpdate) {
            String discoveryFQDN = im.getDiscoveryFQDN();
            String zoneFromDisk = instanceMetaDataService.getAvailabilityZoneFromDiskIfRepair(stackView, Boolean.TRUE, hostGroupName, discoveryFQDN);
            if (StringUtils.isNotEmpty(zoneFromDisk)) {
                LOGGER.info("Setting availability zone to '{}' for instance with FQDN '{}' in group '{}' during repair", zoneFromDisk, discoveryFQDN,
                        hostGroupName);
                setSubnet(im, stack, zoneFromDisk, stackLevelSubnetId);
                im.setAvailabilityZone(zoneFromDisk);
                im.setRackId("/" +
                        (isNullOrEmpty(zoneFromDisk) ?
                                (isNullOrEmpty(im.getInstanceId()) ? DEFAULT_RACK : im.getSubnetId())
                                : zoneFromDisk));
                updatedInstances.add(im);
            } else {
                LOGGER.info("Availability zone not found for instance(fqdn: {}) based on disk.", discoveryFQDN);
                hasInstanceWithoutDiskWithAZ = true;
            }
        }
        if (hasInstanceWithoutDiskWithAZ) {
            InstanceGroup instanceGroup = notDeletedInstancesForGroup.stream()
                    .filter(im -> hostGroupName.equals(im.getInstanceGroup().getGroupName()))
                    .findFirst()
                    .map(InstanceMetaData::getInstanceGroup)
                    .get();
            Set<String> zonesOfInstanceGroup = getZonesOfInstanceGroupOrFromScaleDetails(stack, instanceGroup, networkScaleDetails);
            updatedInstances.addAll(
                    populateAvailabilityZoneOfInstances(
                            zonesOfInstanceGroup,
                            notDeletedInstancesForGroup,
                            instanceGroup,
                            stackLevelSubnetId,
                            stack.getNetwork()
                    )
            );
        }
        return updatedInstances;
    }

    private void setSubnet(InstanceMetaData im, StackDtoDelegate stackDtoDelegate, String zoneFromDisk, String stackLevelSubnetId) {
        if (ZONAL_SUBNET_CLOUD_PLATFORMS.contains(stackDtoDelegate.getCloudPlatform())) {
            Map<String, String> subnetAzMap = getSubnetAzMapFromEnvironment(im.getInstanceGroup(), stackDtoDelegate.getNetwork());
            String subnetId = subnetAzMap.entrySet().stream()
                    .filter(e -> e.getValue().equals(zoneFromDisk))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow(() -> new CloudbreakServiceException(String.format("The zone %s has no assigned subnet on environment.", zoneFromDisk)));
            im.setSubnetId(subnetId);
        } else if (StringUtils.isEmpty(im.getSubnetId())) {
            im.setSubnetId(stackLevelSubnetId);
        }
    }

    private Set<InstanceMetaData> populateOnInstancesOfGroupForScaling(StackDtoDelegate stack, String hostGroupName,
            Set<InstanceMetaData> notDeletedInstancesForGroup, NetworkScaleDetails networkScaleDetails) {
        LOGGER.info("Populating availability zones of instances for upscale in group: '{}'", hostGroupName);
        String stackLevelSubnetId = getStackSubnetIdIfExists(stack);
        InstanceGroup instanceGroup = notDeletedInstancesForGroup.stream()
                .filter(im -> hostGroupName.equals(im.getInstanceGroup().getGroupName()))
                .findFirst()
                .map(InstanceMetaData::getInstanceGroup)
                .get();
        Set<String> zonesOfInstanceGroup = getZonesOfInstanceGroupOrFromScaleDetails(stack, instanceGroup, networkScaleDetails);
        return populateAvailabilityZoneOfInstances(
                zonesOfInstanceGroup,
                notDeletedInstancesForGroup,
                instanceGroup,
                stackLevelSubnetId,
                stack.getNetwork(),
                new HashSet<>(networkScaleDetails.getPreferredSubnetIds())
        );
    }

    private Set<String> getZonesOfInstanceGroupOrFromScaleDetails(StackDtoDelegate stack, InstanceGroup instanceGroup,
            NetworkScaleDetails networkScaleDetails) {
        Set<String> zones = new HashSet<>();
        String groupName = instanceGroup.getGroupName();
        if (CollectionUtils.isEmpty(networkScaleDetails.getPreferredAvailabilityZones()) && ZONAL_SUBNET_CLOUD_PLATFORMS.contains(stack.getCloudPlatform())) {
            Set<String> subnetIdsToCollectZonesFrom = new HashSet<>();
            if (CollectionUtils.isNotEmpty(networkScaleDetails.getPreferredSubnetIds())) {
                LOGGER.debug("Adding preferred subnet ids({}) for group: '{}'", String.join(",", networkScaleDetails.getPreferredSubnetIds()), groupName);
                subnetIdsToCollectZonesFrom.addAll(networkScaleDetails.getPreferredSubnetIds());
            } else {
                Set<String> instanceGroupSubnetIds = instanceGroupSubnetCollector.collect(instanceGroup, stack.getNetwork());
                LOGGER.debug("Adding subnet ids({}) from instance group level network for group: '{}'", String.join(",", instanceGroupSubnetIds), groupName);
                subnetIdsToCollectZonesFrom.addAll(instanceGroupSubnetIds);
            }
            zones.addAll(collectAvailabilityZonesOfSubnetsFromEnv(stack.getEnvironmentCrn(), subnetIdsToCollectZonesFrom));
        } else {
            zones.addAll(CollectionUtils.isEmpty(networkScaleDetails.getPreferredAvailabilityZones()) ?
                    stack.getAvailabilityZonesByInstanceGroup(instanceGroup.getId())
                    : networkScaleDetails.getPreferredAvailabilityZones());
        }
        LOGGER.info("Collected zones: '{}' for group: {}", String.join(",", zones), groupName);
        return zones;
    }

    private Set<String> collectAvailabilityZonesOfSubnetsFromEnv(String environmentCrn, Set<String> subnetIds) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse(environmentCrn);
        return detailedEnvironmentResponse.getNetwork().getSubnetMetas().entrySet().stream()
                .filter(subnetMeta -> subnetIds.contains(subnetMeta.getKey()))
                .map(entry -> entry.getValue().getAvailabilityZone())
                .collect(Collectors.toSet());
    }
}