package com.sequenceiq.datalake.service.upgrade;

import static com.sequenceiq.common.api.type.InstanceGroupName.AUXILIARY;
import static com.sequenceiq.common.api.type.InstanceGroupName.CORE;
import static com.sequenceiq.common.api.type.InstanceGroupName.GATEWAY;
import static com.sequenceiq.common.api.type.InstanceGroupName.IDBROKER;
import static com.sequenceiq.common.api.type.InstanceGroupName.MASTER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;

@Component
public class EnterpriseOrderedOSUpgrade extends AbstractOrderedOSUpgrade {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnterpriseOrderedOSUpgrade.class);

    @Override
    public List<OrderedOSUpgradeSet> createDatalakeOrderedOSUpgrade(StackV4Response stackV4Response, String targetImageId) {
        Map<String, List<InstanceMetaDataV4Response>> instanceMetaDataByInstanceGroup = getInstanceMetaDataByInstanceGroup(stackV4Response);
        Map<String, List<String>> instanceIdsByInstanceGroup = getInstanceIdsByInstanceGroup(instanceMetaDataByInstanceGroup);
        LOGGER.debug("Instance ids by instance group: {}", instanceIdsByInstanceGroup);

        int order = 0;
        List<OrderedOSUpgradeSet> osUpgradeByUpgradeSets = new ArrayList<>();
        osUpgradeByUpgradeSets.add(new OrderedOSUpgradeSet(order++, Set.of(
                pollInstanceId(instanceIdsByInstanceGroup, MASTER),
                pollInstanceId(instanceIdsByInstanceGroup, CORE),
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
                pollInstanceId(instanceIdsByInstanceGroup, GATEWAY),
                pollInstanceId(instanceIdsByInstanceGroup, AUXILIARY)
        )));

        addServiceHostGroupsToOrderedOSUpgradeSet(instanceIdsByInstanceGroup, order, osUpgradeByUpgradeSets);
        validateThatEveryInstanceIsPresentInTheConfig(instanceIdsByInstanceGroup);
        return osUpgradeByUpgradeSets;
    }
}
