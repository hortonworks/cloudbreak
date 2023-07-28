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
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class InstanceMetadataAvailabilityZoneCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetadataAvailabilityZoneCalculator.class);

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    public void populate(Long stackId) {
        Stack stack = stackService.getByIdWithLists(stackId);
        if (stack.isMultiAz() && availabilityZoneConnectorExistsForPlatform(stack)) {
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

    private boolean availabilityZoneConnectorExistsForPlatform(Stack stack) {
        LOGGER.debug("CloudPlatform is {} PlatformVariant is {}", stack.getCloudPlatform(), stack.getPlatformVariant());
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(stack.getCloudPlatform()),
                Variant.variant(stack.getPlatformVariant()));
        return Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant).availabilityZoneConnector()).isPresent();
    }

    private Set<InstanceMetaData> populateAvailabilityZonesOnGroup(InstanceGroup instanceGroup) {
        String groupName = instanceGroup.getGroupName();
        Set<String> availabilityZonesForGroup = instanceGroup.getAvailabilityZones();
        if (CollectionUtils.isNotEmpty(availabilityZonesForGroup)) {
            LOGGER.info("Availability zones to distribute the instances over: '{}'", availabilityZonesForGroup);
            Set<InstanceMetaData> instancesInGroup = instanceGroup.getNotDeletedInstanceMetaDataSet();
            return populateAvailabilityZoneOfInstances(availabilityZonesForGroup, instancesInGroup, groupName);
        } else {
            String msg = String.format("Multi-AZ is enabled for the stack, but no availability zone is available for the group: '%s'", groupName);
            LOGGER.warn(msg);
            throw new CloudbreakServiceException(msg);
        }
    }

    private Set<InstanceMetaData> populateAvailabilityZoneOfInstances(Set<String> availabilityZonesForGroup, Set<InstanceMetaData> instanceMetaDataSet,
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
                updatedInstances.add(instance);
                LOGGER.info("Set availability zone('{}') for instance '{}'", instance.getAvailabilityZone(), instance.getInstanceId());
            }
        }
        return updatedInstances;
    }

    private long countInstancesForAvailabilityZone(Set<InstanceMetaData> instanceMetaDataSet, String availabilityZone) {
        return instanceMetaDataSet.stream().filter(instance -> availabilityZone.equals(instance.getAvailabilityZone())).count();
    }

    private void updateInstancesMetaData(Set<InstanceMetaData> instancesToBeUpdated) {
        if (CollectionUtils.isNotEmpty(instancesToBeUpdated)) {
            LOGGER.debug("Saving instance meta data set with populated availability zones.");
            instanceMetaDataService.saveAll(instancesToBeUpdated);
        }
    }
}
