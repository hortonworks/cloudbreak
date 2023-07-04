package com.sequenceiq.datalake.service.upgrade;

import static com.sequenceiq.common.api.type.InstanceGroupName.ATLASHG;
import static com.sequenceiq.common.api.type.InstanceGroupName.AUXILIARY;
import static com.sequenceiq.common.api.type.InstanceGroupName.CORE;
import static com.sequenceiq.common.api.type.InstanceGroupName.GATEWAY;
import static com.sequenceiq.common.api.type.InstanceGroupName.HMSHG;
import static com.sequenceiq.common.api.type.InstanceGroupName.IDBROKER;
import static com.sequenceiq.common.api.type.InstanceGroupName.KAFKAHG;
import static com.sequenceiq.common.api.type.InstanceGroupName.MASTER;
import static com.sequenceiq.common.api.type.InstanceGroupName.RAZHG;
import static com.sequenceiq.common.api.type.InstanceGroupName.SOLRHG;
import static com.sequenceiq.common.api.type.InstanceGroupName.STORAGEHG;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSetRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.InstanceGroupName;

@Component
public class OrderedOSUpgradeRequestProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderedOSUpgradeRequestProvider.class);

    public OrderedOSUpgradeSetRequest createDatalakeOrderedOSUpgradeSetRequest(StackV4Response stackV4Response, String targetImageId) {
        LOGGER.debug("Creating OrderedOSUpgradeSetRequest for rolling OS upgrade");
        Map<String, List<InstanceMetaDataV4Response>> instanceMetaDataByInstanceGroup = getInstanceMetaDataByInstanceGroup(stackV4Response);
        Map<String, List<String>> instanceIdsByInstanceGroup = getInstanceIdsByInstanceGroup(instanceMetaDataByInstanceGroup);
        LOGGER.debug("Instance ids by instance group: {}", instanceIdsByInstanceGroup);

        int order = 0;
        List<OrderedOSUpgradeSet> osUpgradeByUpgradeSets = new ArrayList<>();
        osUpgradeByUpgradeSets.add(new OrderedOSUpgradeSet(order++, Set.of(
                pollInstanceId(instanceIdsByInstanceGroup, MASTER),
                pollInstanceId(instanceIdsByInstanceGroup, CORE),
                pollInstanceId(instanceIdsByInstanceGroup, AUXILIARY),
                pollInstanceId(instanceIdsByInstanceGroup, IDBROKER)
        )));
        osUpgradeByUpgradeSets.add(new OrderedOSUpgradeSet(order++, Set.of(
                pollInstanceId(instanceIdsByInstanceGroup, MASTER),
                pollInstanceId(instanceIdsByInstanceGroup, CORE),
                pollInstanceId(instanceIdsByInstanceGroup, GATEWAY),
                pollInstanceId(instanceIdsByInstanceGroup, IDBROKER)
        )));
        osUpgradeByUpgradeSets.add(new OrderedOSUpgradeSet(order++, Set.of(
                pollInstanceId(instanceIdsByInstanceGroup, CORE),
                pollInstanceId(instanceIdsByInstanceGroup, GATEWAY)
        )));
        addServiceHostGroupsToOrderedOSUpgradeSet(instanceIdsByInstanceGroup, order, osUpgradeByUpgradeSets);

        validateThatEveryInstanceIsPresentInTheConfig(instanceIdsByInstanceGroup);
        OrderedOSUpgradeSetRequest request = new OrderedOSUpgradeSetRequest();
        request.setOrderedOsUpgradeSets(osUpgradeByUpgradeSets);
        request.setImageId(targetImageId);
        LOGGER.debug("Request created for rolling OS upgrade: {}", request);
        return request;
    }

    private Map<String, List<InstanceMetaDataV4Response>> getInstanceMetaDataByInstanceGroup(StackV4Response stackV4Response) {
        return stackV4Response.getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getMetadata().stream().sorted(Comparator.comparing(InstanceMetaDataV4Response::getDiscoveryFQDN)))
                .collect(Collectors.groupingBy(InstanceMetaDataV4Response::getInstanceGroup));
    }

    private Map<String, List<String>> getInstanceIdsByInstanceGroup(Map<String, List<InstanceMetaDataV4Response>> instanceMetaDataByInstanceGroup) {
        return instanceMetaDataByInstanceGroup.entrySet().stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(InstanceMetaDataV4Response::getInstanceId)
                                .collect(Collectors.toList())));
    }

    private String pollInstanceId(Map<String, List<String>> instanceIdsByInstanceGroup, InstanceGroupName instanceGroup) {
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

    private void validateThatEveryInstanceIsPresentInTheConfig(Map<String, List<String>> instanceIdsByInstanceGroup) {
        Set<String> instancesFromInstanceGroups = instanceIdsByInstanceGroup.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toSet());
        if (!instancesFromInstanceGroups.isEmpty()) {
            throw new CloudbreakServiceException(
                    String.format("The following instances are missing from the ordered OS upgrade request: %s", instancesFromInstanceGroups));
        }
    }

    private void addServiceHostGroupsToOrderedOSUpgradeSet(Map<String, List<String>> instanceIdsByInstanceGroup, int order,
            List<OrderedOSUpgradeSet> osUpgradeByUpgradeSets) {
        Set<String> instanceIds = collectInstanceIdsFromInstanceGroups(instanceIdsByInstanceGroup);
        while (!instanceIds.isEmpty()) {
            osUpgradeByUpgradeSets.add(new OrderedOSUpgradeSet(order++, instanceIds));
            instanceIds = collectInstanceIdsFromInstanceGroups(instanceIdsByInstanceGroup);
        }
    }

    private Set<String> collectInstanceIdsFromInstanceGroups(Map<String, List<String>> instanceIdsByInstanceGroup) {
        Set<String> instanceIds = new HashSet<>();
        pollInstanceIdFromServiceHostGroup(instanceIdsByInstanceGroup, SOLRHG, instanceIds);
        pollInstanceIdFromServiceHostGroup(instanceIdsByInstanceGroup, STORAGEHG, instanceIds);
        pollInstanceIdFromServiceHostGroup(instanceIdsByInstanceGroup, KAFKAHG, instanceIds);
        pollInstanceIdFromServiceHostGroup(instanceIdsByInstanceGroup, RAZHG, instanceIds);
        pollInstanceIdFromServiceHostGroup(instanceIdsByInstanceGroup, ATLASHG, instanceIds);
        pollInstanceIdFromServiceHostGroup(instanceIdsByInstanceGroup, HMSHG, instanceIds);
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
