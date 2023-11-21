package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPGRADE_STARTED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class UpgradeDatalakeFlowEventChainFactory implements FlowEventChainFactory<ClusterUpgradeTriggerEvent>, ClusterUseCaseAware {

    @Value("${cb.upgrade.validation.sdx.enabled}")
    private boolean upgradeValidationEnabled;

    @Inject
    private LockedComponentService lockedComponentService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ClusterUpgradeTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        addUpgradeValidationToChain(event, flowEventChain);
        flowEventChain.add(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new ClusterUpgradeTriggerEvent(CLUSTER_UPGRADE_INIT_EVENT.event(), event.getResourceId(),
                event.accepted(), event.getImageId(), event.isRollingUpgradeEnabled()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    @Override
    public CDPClusterStatus.Value getUseCaseForFlowState(Enum flowState) {
        if (SaltUpdateState.INIT_STATE.equals(flowState)) {
            return UPGRADE_STARTED;
        } else if (ClusterUpgradeState.CLUSTER_UPGRADE_FINISHED_STATE.equals(flowState)) {
            return UPGRADE_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return UPGRADE_FAILED;
        } else {
            return UNSET;
        }
    }

    private void addUpgradeValidationToChain(ClusterUpgradeTriggerEvent event, Queue<Selectable> flowEventChain) {
        if (upgradeValidationEnabled) {
            StackDto stack = stackDtoService.getById(event.getResourceId());
            boolean lockComponents = lockedComponentService.isComponentsLocked(stack, event.getImageId());
            flowEventChain.add(new ClusterUpgradeValidationTriggerEvent(event.getResourceId(), event.accepted(), event.getImageId(), lockComponents,
                    event.isRollingUpgradeEnabled()));
        }
    }
}
