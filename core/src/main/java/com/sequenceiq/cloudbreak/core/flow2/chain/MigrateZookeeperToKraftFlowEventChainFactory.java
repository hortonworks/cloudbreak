package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.KRAFT_MIGRATION_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPSCALE_FAILED;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftConfigurationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftMigrationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class MigrateZookeeperToKraftFlowEventChainFactory implements FlowEventChainFactory<MigrateZookeeperToKraftFlowChainTriggerEvent>,
        ClusterUseCaseAware {

    private static final String KRAFT_HOST_GROUP_NAME = "kraft";

    private static final int KRAFT_HOST_GROUP_SIZE = 3;

    @Inject
    private StackService stackService;

    @Inject
    private MigrateZookeeperToKraftConfigurationFlowConfig configurationFlowConfig;

    @Inject
    private MigrateZookeeperToKraftMigrationFlowConfig migrationFlowConfig;

    @Inject
    private UpscaleFlowEventChainFactory upscaleFlowEventChainFactory;

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (flowState instanceof MigrateZookeeperToKraftConfigurationState) {
            return configurationFlowConfig.getUseCaseForFlowState(flowState);
        } else if (flowState instanceof MigrateZookeeperToKraftMigrationState) {
            return migrationFlowConfig.getUseCaseForFlowState(flowState);
        } else {
            return upscaleFlowEventChainFactory.getUseCaseForFlowState(flowState) == UPSCALE_FAILED ? KRAFT_MIGRATION_FAILED : UNSET;
        }
    }

    @Override
    public String initEvent() {
        return FlowChainTriggers.MIGRATE_ZOOKEEPER_TO_KRAFT_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(MigrateZookeeperToKraftFlowChainTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        boolean kraftHostGroupPresent = isKraftHostGroupPresent(event.getResourceId());
        boolean kraftInstallNeeded = !kraftHostGroupPresent;
        boolean kraftUpscaleNeeded = false;
        flowEventChain.add(getKraftMigrationConfigurationTriggerEvent(event, kraftInstallNeeded));
        if (kraftHostGroupPresent) {
            int kraftNodeCount = getKraftNodeCountByStackId(event.getResourceId());
            kraftUpscaleNeeded = isKraftUpscaleNeeded(kraftNodeCount);
            int nodeAdjustment = getKraftNodeAdjustment(kraftNodeCount);
            if (kraftUpscaleNeeded) {
                flowEventChain.add(getStackUpscaleTriggerEvent(event, nodeAdjustment));
            }
        }
        boolean staleConfigsOnly = !kraftUpscaleNeeded && !kraftInstallNeeded;
        flowEventChain.add(getKraftMigrationTriggerEvent(event, staleConfigsOnly, kraftHostGroupPresent));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private boolean isKraftUpscaleNeeded(int kraftNodeCount) {
        return kraftNodeCount < KRAFT_HOST_GROUP_SIZE;
    }

    private boolean isKraftHostGroupPresent(long stackId) {
        return stackService.getByIdWithLists(stackId).getInstanceGroups().stream().anyMatch(ig -> KRAFT_HOST_GROUP_NAME.equalsIgnoreCase(ig.getGroupName()));
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

    private Selectable getKraftMigrationConfigurationTriggerEvent(MigrateZookeeperToKraftFlowChainTriggerEvent event, boolean kraftInstallNeeded) {
        Long stackId = event.getResourceId();
        return new MigrateZookeeperToKraftConfigurationTriggerEvent(stackId, kraftInstallNeeded, event.accepted());
    }

    private Selectable getStackUpscaleTriggerEvent(MigrateZookeeperToKraftFlowChainTriggerEvent event, int nodeAdjustment) {
        CloudPlatformVariant variant = stackService.getPlatformVariantByStackId(event.getResourceId());
        return new StackAndClusterUpscaleTriggerEvent(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.getResourceId(),
                Collections.singletonMap(KRAFT_HOST_GROUP_NAME, nodeAdjustment), Collections.emptyMap(), Collections.emptyMap(),
                ScalingType.UPSCALE_TOGETHER, false, false, event.accepted(), false, false,
                ClusterManagerType.CLOUDERA_MANAGER, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, (long) nodeAdjustment),
                variant.getVariant().value(), false);
    }

    private Selectable getKraftMigrationTriggerEvent(MigrateZookeeperToKraftFlowChainTriggerEvent event, boolean staleConfigsOnly,
            boolean kraftHostGroupPresent) {
        Long stackId = event.getResourceId();
        return new MigrateZookeeperToKraftTriggerEvent(stackId, staleConfigsOnly, kraftHostGroupPresent, event.accepted());
    }
}
