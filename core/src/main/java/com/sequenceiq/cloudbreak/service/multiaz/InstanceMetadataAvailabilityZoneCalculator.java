package com.sequenceiq.cloudbreak.service.multiaz;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.StackView;

public class InstanceMetadataAvailabilityZoneCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetadataAvailabilityZoneCalculator.class);

    private static final String GROUP_NOT_EXISTS_WITH_NAME_PATTERN = "Invalid state: instance group with name '%s' has been requested to be scaled, "
            + "but the cluster doesn't contain group with that name.";

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private MultiAzCalculatorService multiAzCalculatorService;

    public void populate(Long stackId) {
        Stack stack = stackService.getByIdWithLists(stackId);
        populate(stack);
    }

    public boolean populateForScaling(StackDtoDelegate stack, Set<String> hostGroupsWithInstancesToCreate, boolean repair) {
        boolean updateHappened = Boolean.FALSE;
        if (populateSupportedOnStack(stack)) {
            Set<InstanceMetaData> updatedInstances = new HashSet<>();
            Set<InstanceMetaData> notDeletedInstances = instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(stack.getId());
            for (String hostGroupName : hostGroupsWithInstancesToCreate) {
                populateOnGroupForScalingAndRepair(stack, repair, hostGroupName, notDeletedInstances, updatedInstances);
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
            Set<InstanceMetaData> instancesToBeUpdated = new HashSet<>();
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                instancesToBeUpdated.addAll(populateAvailabilityZonesOnGroup(instanceGroup));
            }
            updateInstancesMetaData(instancesToBeUpdated);
        } else {
            LOGGER.debug("Stack is NOT multi-AZ enabled or AvailabilityZone connector doesn't exist for platform , no need to populate zones for instances");
        }
    }

    protected Set<InstanceMetaData> populateAvailabilityZonesOnGroup(InstanceGroup instanceGroup) {
        String groupName = instanceGroup.getGroupName();
        Set<String> availabilityZonesForGroup = instanceGroup.getAvailabilityZones();
        if (CollectionUtils.isNotEmpty(availabilityZonesForGroup)) {
            LOGGER.info("Availability zones to distribute the instances over: '{}' for group: '{}'", availabilityZonesForGroup, groupName);
            Set<InstanceMetaData> instancesInGroup = instanceGroup.getNotDeletedInstanceMetaDataSet();
            return populateAvailabilityZoneOfInstances(availabilityZonesForGroup, instancesInGroup, groupName);
        } else {
            String msg = String.format("Multi-AZ is enabled for the stack, but no availability zone is available for the group: '%s'", groupName);
            LOGGER.warn(msg);
            throw new CloudbreakServiceException(msg);
        }
    }

    protected Set<InstanceMetaData> populateAvailabilityZoneOfInstances(Set<String> availabilityZonesForGroup, Set<InstanceMetaData> instanceMetaDataSet,
            String groupName) {
        Set<InstanceMetaData> updatedInstances = new HashSet<>();
        Map<String, Long> zoneToNodeCountMap = availabilityZonesForGroup.stream().collect(Collectors.toMap(Function.identity(),
                availabilityZone -> countInstancesForAvailabilityZone(instanceMetaDataSet, availabilityZone)));
        LOGGER.debug("Initialized zoneToNodeCountMap {} for group '{}'", zoneToNodeCountMap, groupName);
        for (InstanceMetaData instance : instanceMetaDataSet) {
            if (StringUtils.isEmpty(instance.getAvailabilityZone())) {
                String availabilityZone = Collections.min(zoneToNodeCountMap.entrySet(), Map.Entry.comparingByValue()).getKey();
                zoneToNodeCountMap.put(availabilityZone, zoneToNodeCountMap.get(availabilityZone) + 1);
                instance.setAvailabilityZone(availabilityZone);
                String previousRackId = instance.getRackId();
                instance.setRackId(multiAzCalculatorService.determineRackId(instance.getSubnetId(), availabilityZone));
                updatedInstances.add(instance);
                LOGGER.info("Set availability zone('{}') for instance '{}'", instance.getAvailabilityZone(), instance.getInstanceId());
                LOGGER.info("Rack Id updated from {} to {}", previousRackId, instance.getRackId());
            }
        }
        return updatedInstances;
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
            Set<InstanceMetaData> updatedInstances) {
        LOGGER.debug("Populating availability zone for instances in group: '{}'", hostGroupName);
        InstanceGroupDto instanceGroupDto = getInstanceGroupDtoByGroupName(stack, hostGroupName);
        Long instanceGroupId = instanceGroupDto.getInstanceGroup().getId();
        Set<InstanceMetaData> notDeletedInstancesForGroup = getInstancesByInstanceGroupId(notDeletedInstances, instanceGroupId);
        if (repair) {
            updatedInstances.addAll(populateOnInstancesOfGroupForRepair(stack, hostGroupName, notDeletedInstancesForGroup));
        } else {
            updatedInstances.addAll(populateOnInstancesOfGroupForScaling(stack, hostGroupName, instanceGroupId, notDeletedInstancesForGroup));
        }
    }

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

    private Set<InstanceMetaData> populateOnInstancesOfGroupForRepair(StackDtoDelegate stack, String hostGroupName,
            Set<InstanceMetaData> notDeletedInstancesForGroup) {
        LOGGER.info("Populating availability zones of instances for repair in group: '{}'", hostGroupName);
        Set<InstanceMetaData> updatedInstances = new HashSet<>();
        StackView stackView = stack.getStack();
        Set<InstanceMetaData> instancesToUpdate = notDeletedInstancesForGroup.stream()
                .filter(im -> InstanceStatus.REQUESTED.equals(im.getInstanceStatus()) && StringUtils.isEmpty(im.getAvailabilityZone()))
                .collect(Collectors.toSet());
        for (InstanceMetaData im : instancesToUpdate) {
            String discoveryFQDN = im.getDiscoveryFQDN();
            String zoneFromDisk = instanceMetaDataService.getAvailabilityZoneFromDiskIfRepair(stackView, Boolean.TRUE, hostGroupName, discoveryFQDN);
            if (StringUtils.isNotEmpty(zoneFromDisk)) {
                LOGGER.info("Setting availability zone to '{}' for instance with FQDN '{}' in group '{}' during repair", zoneFromDisk, discoveryFQDN,
                        hostGroupName);
                im.setAvailabilityZone(zoneFromDisk);
                im.setRackId(multiAzCalculatorService.determineRackId(im.getSubnetId(), zoneFromDisk));
                updatedInstances.add(im);
            } else {
                String msg = String.format("Repair could not be initiated because availability zone could not be found for instances as no disk " +
                        "resource in our metadata for instance with FQDN '%s' in group '%s'.", discoveryFQDN, hostGroupName);
                LOGGER.warn(msg);
                throw new CloudbreakServiceException(msg);
            }
        }
        return updatedInstances;
    }

    private Set<InstanceMetaData> populateOnInstancesOfGroupForScaling(StackDtoDelegate stack, String hostGroupName, Long instanceGroupId,
            Set<InstanceMetaData> notDeletedInstancesForGroup) {
        LOGGER.info("Populating availability zones of instances for upscale in group: '{}'", hostGroupName);
        Set<String> zonesOfInstanceGroup = stack.getAvailabilityZonesByInstanceGroup(instanceGroupId);
        return populateAvailabilityZoneOfInstances(zonesOfInstanceGroup, notDeletedInstancesForGroup, hostGroupName);
    }
}
