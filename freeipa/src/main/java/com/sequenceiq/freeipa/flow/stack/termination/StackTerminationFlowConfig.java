package com.sequenceiq.freeipa.flow.stack.termination;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.DELETE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.DELETE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.DELETE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.CCM_KEY_DEREGISTRATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.CLUSTER_PROXY_DEREGISTRATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.DELETE_USERDATA_SECRETS_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.DELETE_USERDATA_SECRETS_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.EXECUTE_PRE_TERMINATION_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.REMOVE_MACHINE_USER_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.STOP_TELEMETRY_AGENT_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.DELETE_USERDATA_SECRETS_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.DEREGISTER_CCMKEY_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.DEREGISTER_CLUSTERPROXY_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.EXECUTE_PRE_TERMINATION_RECIPES;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.REMOVE_MACHINE_USER_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.STOP_TELEMETRY_AGENT_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.TERMINATION_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.TERMINATION_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.TERMINATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class StackTerminationFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<StackTerminationState, StackTerminationEvent>
        implements FreeIpaUseCaseAware {

    private static final List<Transition<StackTerminationState, StackTerminationEvent>> TRANSITIONS =
            new Builder<StackTerminationState, StackTerminationEvent>()
                    .defaultFailureEvent(TERMINATION_FAILED_EVENT)
                    .from(INIT_STATE).to(EXECUTE_PRE_TERMINATION_RECIPES).event(TERMINATION_EVENT).defaultFailureEvent()
                    .from(EXECUTE_PRE_TERMINATION_RECIPES).to(DELETE_USERDATA_SECRETS_STATE).event(EXECUTE_PRE_TERMINATION_RECIPES_FINISHED_EVENT)
                    .defaultFailureEvent()
                    .from(DELETE_USERDATA_SECRETS_STATE).to(STOP_TELEMETRY_AGENT_STATE).event(DELETE_USERDATA_SECRETS_FINISHED_EVENT)
                    .failureEvent(DELETE_USERDATA_SECRETS_FAILED_EVENT)
                    .from(STOP_TELEMETRY_AGENT_STATE).to(DEREGISTER_CLUSTERPROXY_STATE).event(STOP_TELEMETRY_AGENT_FINISHED_EVENT).defaultFailureEvent()
                    .from(DEREGISTER_CLUSTERPROXY_STATE).to(DEREGISTER_CCMKEY_STATE).event(CLUSTER_PROXY_DEREGISTRATION_FINISHED_EVENT).defaultFailureEvent()
                    .from(DEREGISTER_CCMKEY_STATE).to(REMOVE_MACHINE_USER_STATE).event(CCM_KEY_DEREGISTRATION_FINISHED_EVENT).defaultFailureEvent()
                    .from(REMOVE_MACHINE_USER_STATE).to(TERMINATION_STATE).event(REMOVE_MACHINE_USER_FINISHED_EVENT).defaultFailureEvent()
                    .from(TERMINATION_STATE).to(TERMINATION_FINISHED_STATE).event(TERMINATION_FINISHED_EVENT).defaultFailureEvent()
                    .from(TERMINATION_FINISHED_STATE).to(FINAL_STATE).event(TERMINATION_FINALIZED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<StackTerminationState, StackTerminationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, TERMINATION_FAILED_STATE, STACK_TERMINATION_FAIL_HANDLED_EVENT);

    public StackTerminationFlowConfig() {
        super(StackTerminationState.class, StackTerminationEvent.class);
    }

    @Override
    protected List<Transition<StackTerminationState, StackTerminationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<StackTerminationState, StackTerminationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public StackTerminationEvent[] getEvents() {
        return StackTerminationEvent.values();
    }

    @Override
    public StackTerminationEvent[] getInitEvents() {
        return new StackTerminationEvent[]{TERMINATION_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Terminate stack";
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return DELETE_STARTED;
        } else if (TERMINATION_FINISHED_STATE.equals(flowState)) {
            return DELETE_FINISHED;
        } else if (TERMINATION_FAILED_STATE.equals(flowState)) {
            return DELETE_FAILED;
        } else {
            return UNSET;
        }
    }
}
