package com.sequenceiq.datalake.service.upgrade;

import static com.sequenceiq.common.api.type.InstanceGroupName.AUXILIARY;
import static com.sequenceiq.common.api.type.InstanceGroupName.CORE;
import static com.sequenceiq.common.api.type.InstanceGroupName.GATEWAY;
import static com.sequenceiq.common.api.type.InstanceGroupName.IDBROKER;
import static com.sequenceiq.common.api.type.InstanceGroupName.MASTER;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    public OrderedOSUpgradeSetRequest createMediumDutyOrderedOSUpgradeSetRequest(StackV4Response stackV4Response) {
        LOGGER.debug("Creating OrderedOSUpgradeSetRequest for rolling OS upgrade");
        Map<String, List<InstanceMetaDataV4Response>> instanceMetaDataByInstanceGroup = getInstanceMetaDataByInstanceGroup(stackV4Response);
        Map<String, List<String>> instanceIdsByInstanceGroup = getInstanceIdsByInstanceGroup(instanceMetaDataByInstanceGroup);
        LOGGER.debug("Instance ids by instance group: {}", instanceIdsByInstanceGroup);

        List<OrderedOSUpgradeSet> osUpgradeByUpgradeSets = new ArrayList<>();
        osUpgradeByUpgradeSets.add(new OrderedOSUpgradeSet(0, Set.of(
                getInstanceId(instanceIdsByInstanceGroup, MASTER),
                getInstanceId(instanceIdsByInstanceGroup, CORE),
                getInstanceId(instanceIdsByInstanceGroup, AUXILIARY),
                getInstanceId(instanceIdsByInstanceGroup, IDBROKER)
        )));
        osUpgradeByUpgradeSets.add(new OrderedOSUpgradeSet(1, Set.of(
                getInstanceId(instanceIdsByInstanceGroup, MASTER),
                getInstanceId(instanceIdsByInstanceGroup, CORE),
                getInstanceId(instanceIdsByInstanceGroup, GATEWAY),
                getInstanceId(instanceIdsByInstanceGroup, IDBROKER)
        )));
        osUpgradeByUpgradeSets.add(new OrderedOSUpgradeSet(2, Set.of(
                getInstanceId(instanceIdsByInstanceGroup, CORE),
                getInstanceId(instanceIdsByInstanceGroup, GATEWAY)
        )));
        validateThatEveryInstanceIsPresentInTheConfig(instanceIdsByInstanceGroup);
        OrderedOSUpgradeSetRequest request = new OrderedOSUpgradeSetRequest();
        request.setOrderedOsUpgradeSets(osUpgradeByUpgradeSets);
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
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(InstanceMetaDataV4Response::getInstanceId)
                                .collect(Collectors.toList())));
    }

    private String getInstanceId(Map<String, List<String>> instanceIdsByInstanceGroup, InstanceGroupName instanceGroup) {
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
}
