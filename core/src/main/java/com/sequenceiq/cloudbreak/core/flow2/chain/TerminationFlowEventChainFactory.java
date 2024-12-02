package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.DELETE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.DELETE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.DELETE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationEvent.START_EXTERNAL_DATABASE_TERMINATION_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class TerminationFlowEventChainFactory implements FlowEventChainFactory<TerminationEvent>, ClusterUseCaseAware {
    @Override
    public String initEvent() {
        return FlowChainTriggers.TERMINATION_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(TerminationEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new TerminationEvent(ClusterTerminationEvent.TERMINATION_EVENT.event(), event.getResourceId(),
                event.getTerminationType(), event.accepted()));
        flowEventChain.add(new TerminationEvent(START_EXTERNAL_DATABASE_TERMINATION_EVENT.event(), event.getResourceId(), event.getTerminationType(),
                event.accepted()));
        flowEventChain.add(new TerminationEvent(StackTerminationEvent.TERMINATION_EVENT.event(), event.getResourceId(), event.getTerminationType(),
                event.accepted()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    @Override
    public Value getUseCaseForFlowState(Enum flowState) {
        if (ClusterTerminationState.INIT_STATE.equals(flowState)) {
            return DELETE_STARTED;
        } else if (StackTerminationState.TERMINATION_FINISHED_STATE.equals(flowState)) {
            return DELETE_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return DELETE_FAILED;
        } else {
            return UNSET;
        }
    }
}
