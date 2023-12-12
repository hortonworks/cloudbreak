package com.sequenceiq.datalake.service.upgrade;

import static com.sequenceiq.common.api.type.InstanceGroupName.ATLAS_SCALE_OUT;
import static com.sequenceiq.common.api.type.InstanceGroupName.HMS_SCALE_OUT;
import static com.sequenceiq.common.api.type.InstanceGroupName.KAFKA_SCALE_OUT;
import static com.sequenceiq.common.api.type.InstanceGroupName.RAZ_SCALE_OUT;
import static com.sequenceiq.common.api.type.InstanceGroupName.SOLR_SCALE_OUT;
import static com.sequenceiq.common.api.type.InstanceGroupName.STORAGE_SCALE_OUT;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.InstanceGroupName;

public abstract class AbstractOrderedOSUpgrade {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrderedOSUpgrade.class);

    public abstract List<OrderedOSUpgradeSet> createDatalakeOrderedOSUpgrade(StackV4Response stackV4Response, String targetImageId);

    protected void validateThatEveryInstanceIsPresentInTheConfig(Map<String, List<String>> instanceIdsByInstanceGroup) {
        Set<String> instancesFromInstanceGroups = instanceIdsByInstanceGroup.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toSet());
        if (!instancesFromInstanceGroups.isEmpty()) {
            throw new CloudbreakServiceException(
                    String.format("The following instances are missing from the ordered OS upgrade request: %s", instancesFromInstanceGroups));
        }
    }

    protected void addServiceHostGroupsToOrderedOSUpgradeSet(Map<String, List<String>> instanceIdsByInstanceGroup, int order,
            List<OrderedOSUpgradeSet> osUpgradeByUpgradeSets) {
        Set<String> instanceIds = collectInstanceIdsFromInstanceGroups(instanceIdsByInstanceGroup);
        while (!instanceIds.isEmpty()) {
            osUpgradeByUpgradeSets.add(new OrderedOSUpgradeSet(order++, instanceIds));
            instanceIds = collectInstanceIdsFromInstanceGroups(instanceIdsByInstanceGroup);
        }
    }

    protected Map<String, List<InstanceMetaDataV4Response>> getInstanceMetaDataByInstanceGroup(StackV4Response stackV4Response) {
        return stackV4Response.getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getMetadata().stream().sorted(Comparator.comparing(InstanceMetaDataV4Response::getDiscoveryFQDN)))
                .collect(Collectors.groupingBy(InstanceMetaDataV4Response::getInstanceGroup));
    }

    protected Map<String, List<String>> getInstanceIdsByInstanceGroup(Map<String, List<InstanceMetaDataV4Response>> instanceMetaDataByInstanceGroup) {
        return instanceMetaDataByInstanceGroup.entrySet().stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(InstanceMetaDataV4Response::getInstanceId)
                                .collect(Collectors.toList())));
    }

    protected String pollInstanceId(Map<String, List<String>> instanceIdsByInstanceGroup, InstanceGroupName instanceGroup) {
        try {
            return instanceIdsByInstanceGroup.get(instanceGroup.getName()).remove(0);
        } catch (Exception e) {
            String message = String.format("There are no instances left in the %s group. "
                    + "Please make sure your cluster template have enough instances for rolling os upgrade. "
                    + "Remaining instances in instance groups: %s", instanceGroup, instanceIdsByInstanceGroup);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    private Set<String> collectInstanceIdsFromInstanceGroups(Map<String, List<String>> instanceIdsByInstanceGroup) {
        Set<String> instanceIds = new HashSet<>();
        pollInstanceIdFromServiceHostGroup(instanceIdsByInstanceGroup, SOLR_SCALE_OUT, instanceIds);
        pollInstanceIdFromServiceHostGroup(instanceIdsByInstanceGroup, STORAGE_SCALE_OUT, instanceIds);
        pollInstanceIdFromServiceHostGroup(instanceIdsByInstanceGroup, KAFKA_SCALE_OUT, instanceIds);
        pollInstanceIdFromServiceHostGroup(instanceIdsByInstanceGroup, RAZ_SCALE_OUT, instanceIds);
        pollInstanceIdFromServiceHostGroup(instanceIdsByInstanceGroup, ATLAS_SCALE_OUT, instanceIds);
        pollInstanceIdFromServiceHostGroup(instanceIdsByInstanceGroup, HMS_SCALE_OUT, instanceIds);
        return instanceIds;
    }

    private void pollInstanceIdFromServiceHostGroup(Map<String, List<String>> instanceIdsByInstanceGroup,
            InstanceGroupName instanceGroupName, Set<String> instanceIds) {
        if (instanceIdsByInstanceGroup.containsKey(instanceGroupName.getName()) &&
                !instanceIdsByInstanceGroup.get(instanceGroupName.getName()).isEmpty()) {
            instanceIds.add(pollInstanceId(instanceIdsByInstanceGroup, instanceGroupName));
        }
    }
}
