package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.DISABLE_ENCRYPTION_PROFILE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.DISABLE_ENCRYPTION_PROFILE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.DISABLE_ENCRYPTION_PROFILE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigurationUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowState;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class DisableEncryptionProfileFlowEventChainFactory implements FlowEventChainFactory<StackEvent>, ClusterUseCaseAware {

    @Override
    public String initEvent() {
        return FlowChainTriggers.DISABLE_ENCRYPTION_PROFILE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(StackEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();

        flowEventChain.add(new StackEvent(PillarConfigurationUpdateEvent.PILLAR_CONFIG_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));

        flowEventChain.add(new StackEvent(RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_TRIGGER_EVENT.event(), event.getResourceId(),
                event.accepted()));

        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (PillarConfigUpdateState.INIT_STATE.equals(flowState)) {
            return DISABLE_ENCRYPTION_PROFILE_STARTED;
        } else if (RestartClusterManagerFlowState.RESTART_CLUSTER_MANAGER_FINISED_STATE.equals(flowState)) {
            return DISABLE_ENCRYPTION_PROFILE_FINISHED;
        } else if (PillarConfigUpdateState.PILLAR_CONFIG_UPDATE_FAILED_STATE.equals(flowState)
                || RestartClusterManagerFlowState.RESTART_CLUSTER_MANAGER_FAILED_STATE.equals(flowState)) {
            return DISABLE_ENCRYPTION_PROFILE_FAILED;
        } else {
            return UNSET;
        }
    }
}
