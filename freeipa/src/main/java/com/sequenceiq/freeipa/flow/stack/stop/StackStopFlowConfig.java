package com.sequenceiq.freeipa.flow.stack.stop;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.SUSPEND_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.SUSPEND_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.SUSPEND_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
import static com.sequenceiq.freeipa.flow.stack.stop.StackStopEvent.STACK_STOP_EVENT;
import static com.sequenceiq.freeipa.flow.stack.stop.StackStopEvent.STACK_STOP_INSTANCES_EVENT;
import static com.sequenceiq.freeipa.flow.stack.stop.StackStopEvent.STOP_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.stop.StackStopEvent.STOP_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.stop.StackStopEvent.STOP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.stop.StackStopState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.stop.StackStopState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.stack.stop.StackStopState.STOP_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.stop.StackStopState.STOP_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.stack.stop.StackStopState.STOP_INSTANCES_STATE;
import static com.sequenceiq.freeipa.flow.stack.stop.StackStopState.STOP_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class StackStopFlowConfig extends StackStatusFinalizerAbstractFlowConfig<StackStopState, StackStopEvent> implements FreeIpaUseCaseAware {

    private static final List<Transition<StackStopState, StackStopEvent>> TRANSITIONS = new Builder<StackStopState, StackStopEvent>()
            .defaultFailureEvent(StackStopEvent.STOP_FAILURE_EVENT)
            .from(INIT_STATE).to(STOP_STATE).event(STACK_STOP_EVENT).noFailureEvent()
            .from(STOP_STATE).to(STOP_INSTANCES_STATE).event(STACK_STOP_INSTANCES_EVENT).defaultFailureEvent()
            .from(STOP_INSTANCES_STATE).to(STOP_FINISHED_STATE).event(STOP_FINISHED_EVENT).defaultFailureEvent()
            .from(STOP_FINISHED_STATE).to(FINAL_STATE).event(STOP_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<StackStopState, StackStopEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, STOP_FAILED_STATE, STOP_FAIL_HANDLED_EVENT);

    public StackStopFlowConfig() {
        super(StackStopState.class, StackStopEvent.class);
    }

    @Override
    public FlowTriggerCondition getFlowTriggerCondition() {
        return getApplicationContext().getBean(StackStopFlowTriggerCondition.class);
    }

    @Override
    public StackStopEvent[] getEvents() {
        return StackStopEvent.values();
    }

    @Override
    public StackStopEvent[] getInitEvents() {
        return new StackStopEvent[]{
                STACK_STOP_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Stop stack";
    }

    @Override
    protected List<Transition<StackStopState, StackStopEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<StackStopState, StackStopEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return SUSPEND_STARTED;
        } else if (STOP_FINISHED_STATE.equals(flowState)) {
            return SUSPEND_FINISHED;
        } else if (STOP_FAILED_STATE.equals(flowState)) {
            return SUSPEND_FAILED;
        } else {
            return UNSET;
        }
    }
}
