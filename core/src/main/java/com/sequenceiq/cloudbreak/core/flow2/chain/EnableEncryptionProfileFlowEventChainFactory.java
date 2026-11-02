package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.ENABLE_ENCRYPTION_PROFILE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.ENABLE_ENCRYPTION_PROFILE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.ENABLE_ENCRYPTION_PROFILE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors.ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigurationUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.EnableEncryptionProfileOnClusterEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowState;
import com.sequenceiq.cloudbreak.core.flow2.event.EnableEncryptionProfileTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class EnableEncryptionProfileFlowEventChainFactory implements FlowEventChainFactory<EnableEncryptionProfileTriggerEvent>, ClusterUseCaseAware {

    @Override
    public String initEvent() {
        return FlowChainTriggers.ENABLE_ENCRYPTION_PROFILE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(EnableEncryptionProfileTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();

        flowEventChain.add(new EnableEncryptionProfileOnClusterEvent(ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.event(), event.getResourceId(),
                event.accepted(), event.getEncryptionProfileCrn()));

        flowEventChain.add(new StackEvent(PillarConfigurationUpdateEvent.PILLAR_CONFIG_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));

        flowEventChain.add(new StackEvent(RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_TRIGGER_EVENT.event(), event.getResourceId(),
                event.accepted()));

        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (EnableEncryptionProfileOnClusterState.INIT_STATE.equals(flowState)) {
            return ENABLE_ENCRYPTION_PROFILE_STARTED;
        } else if (RestartClusterManagerFlowState.RESTART_CLUSTER_MANAGER_FINISED_STATE.equals(flowState)) {
            return ENABLE_ENCRYPTION_PROFILE_FINISHED;
        } else if (EnableEncryptionProfileOnClusterState.ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED_STATE.equals(flowState)
                || PillarConfigUpdateState.PILLAR_CONFIG_UPDATE_FAILED_STATE.equals(flowState)
                || RestartClusterManagerFlowState.RESTART_CLUSTER_MANAGER_FAILED_STATE.equals(flowState)) {
            return ENABLE_ENCRYPTION_PROFILE_FAILED;
        } else {
            return UNSET;
        }
    }
}
