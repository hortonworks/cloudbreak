package com.sequenceiq.freeipa.flow.stack.start;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.RESUME_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.RESUME_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.RESUME_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartEvent.COLLECT_METADATA_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartEvent.COLLECT_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartEvent.STACK_START_EVENT;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartEvent.START_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartEvent.START_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartEvent.START_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartEvent.START_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartEvent.START_SAVE_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartEvent.START_WAIT_UNTIL_AVAILABLE_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartEvent.START_WAIT_UNTIL_AVAILABLE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartState.COLLECTING_METADATA;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartState.START_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartState.START_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartState.START_SAVE_METADATA_STATE;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartState.START_STATE;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartState.START_WAIT_UNTIL_AVAILABLE_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class StackStartFlowConfig extends StackStatusFinalizerAbstractFlowConfig<StackStartState, StackStartEvent> implements FreeIpaUseCaseAware {

    private static final List<Transition<StackStartState, StackStartEvent>> TRANSITIONS = new Builder<StackStartState, StackStartEvent>()
            .defaultFailureEvent(START_FAILURE_EVENT)
            .from(INIT_STATE).to(START_STATE).event(STACK_START_EVENT).noFailureEvent()
            .from(START_STATE).to(COLLECTING_METADATA).event(START_FINISHED_EVENT).defaultFailureEvent()
            .from(COLLECTING_METADATA).to(START_SAVE_METADATA_STATE).event(COLLECT_METADATA_FINISHED_EVENT).failureEvent(COLLECT_METADATA_FAILED_EVENT)
            .from(START_SAVE_METADATA_STATE).to(START_WAIT_UNTIL_AVAILABLE_STATE).event(START_SAVE_METADATA_FINISHED_EVENT).noFailureEvent()
            .from(START_WAIT_UNTIL_AVAILABLE_STATE).to(START_FINISHED_STATE)
            .event(START_WAIT_UNTIL_AVAILABLE_FINISHED_EVENT).failureEvent(START_WAIT_UNTIL_AVAILABLE_FAILED_EVENT)
            .from(START_FINISHED_STATE).to(FINAL_STATE).event(START_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<StackStartState, StackStartEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, START_FAILED_STATE, START_FAIL_HANDLED_EVENT);

    public StackStartFlowConfig() {
        super(StackStartState.class, StackStartEvent.class);
    }

    @Override
    public StackStartEvent[] getEvents() {
        return StackStartEvent.values();
    }

    @Override
    public StackStartEvent[] getInitEvents() {
        return new StackStartEvent[] {
                STACK_START_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Start stack";
    }

    @Override
    protected List<Transition<StackStartState, StackStartEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<StackStartState, StackStartEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return RESUME_STARTED;
        } else if (START_FINISHED_STATE.equals(flowState)) {
            return RESUME_FINISHED;
        } else if (START_FAILED_STATE.equals(flowState)) {
            return RESUME_FAILED;
        } else {
            return UNSET;
        }
    }
}
