package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class MigrateZookeeperToKraftFlowEventChainFactory implements FlowEventChainFactory<MigrateZookeeperToKraftFlowChainTriggerEvent> {
    private static final String KRAFT_HOST_GROUP_NAME = "kraft";

    private static final int KRAFT_HOST_GROUP_SIZE = 3;

    @Inject
    private StackService stackService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.MIGRATE_ZOOKEEPER_TO_KRAFT_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(MigrateZookeeperToKraftFlowChainTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        int kraftNodeCount = getKraftNodeCountByStackId(event.getResourceId());
        int nodeAdjustment = getKraftNodeAdjustment(kraftNodeCount);
        flowEventChain.add(getKraftMigrationConfigurationTriggerEvent(event));
        if (isKraftUpscaleNeeded(kraftNodeCount)) {
            flowEventChain.add(getStackUpscaleTriggerEvent(event, nodeAdjustment));
        }
        flowEventChain.add(getKraftMigrationTriggerEvent(event));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private boolean isKraftUpscaleNeeded(int kraftNodeCount) {
        return kraftNodeCount < KRAFT_HOST_GROUP_SIZE;
    }

    private int getKraftNodeCountByStackId(long stackId) {
        Stack stack = stackService.getByIdWithLists(stackId);
        return stack.getInstanceGroups().stream()
                .filter(ig -> KRAFT_HOST_GROUP_NAME.equalsIgnoreCase(ig.getGroupName()))
                .findFirst()
                .map(InstanceGroup::getNodeCount)
                .orElse(0);
    }

    private int getKraftNodeAdjustment(int kraftNodeCount) {
        return KRAFT_HOST_GROUP_SIZE - kraftNodeCount;
    }

    private Selectable getKraftMigrationConfigurationTriggerEvent(MigrateZookeeperToKraftFlowChainTriggerEvent event) {
        Long stackId = event.getResourceId();
        return new MigrateZookeeperToKraftConfigurationTriggerEvent(stackId, event.accepted());
    }

    private Selectable getStackUpscaleTriggerEvent(MigrateZookeeperToKraftFlowChainTriggerEvent event, int nodeAdjustment) {
        CloudPlatformVariant variant = stackService.getPlatformVariantByStackId(event.getResourceId());
        return new StackAndClusterUpscaleTriggerEvent(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.getResourceId(),
                Collections.singletonMap(KRAFT_HOST_GROUP_NAME, nodeAdjustment), Collections.emptyMap(), Collections.emptyMap(),
                ScalingType.UPSCALE_TOGETHER, false, false, event.accepted(), false, false,
                ClusterManagerType.CLOUDERA_MANAGER, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, (long) nodeAdjustment),
                variant.getVariant().value(), false, true);
    }

    private Selectable getKraftMigrationTriggerEvent(MigrateZookeeperToKraftFlowChainTriggerEvent event) {
        Long stackId = event.getResourceId();
        return new MigrateZookeeperToKraftTriggerEvent(stackId, event.accepted());
    }
}
