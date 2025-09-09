package com.sequenceiq.freeipa.flow.instance.reboot;

import static com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent.REBOOT_EVENT;
import static com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent.REBOOT_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent.REBOOT_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent.REBOOT_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent.REBOOT_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent.REBOOT_WAIT_UNTIL_AVAILABLE_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent.REBOOT_WAIT_UNTIL_AVAILABLE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.instance.reboot.RebootState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.instance.reboot.RebootState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.instance.reboot.RebootState.REBOOT_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.instance.reboot.RebootState.REBOOT_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.instance.reboot.RebootState.REBOOT_STATE;
import static com.sequenceiq.freeipa.flow.instance.reboot.RebootState.REBOOT_WAIT_UNTIL_AVAILABLE_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class RebootFlowConfig extends StackStatusFinalizerAbstractFlowConfig<RebootState, RebootEvent> {

    private static final List<Transition<RebootState, RebootEvent>> TRANSITIONS = new Builder<RebootState, RebootEvent>()
            .defaultFailureEvent(REBOOT_FAILURE_EVENT)
            .from(INIT_STATE).to(REBOOT_STATE).event(REBOOT_EVENT).defaultFailureEvent()
            .from(REBOOT_STATE).to(REBOOT_WAIT_UNTIL_AVAILABLE_STATE).event(REBOOT_FINISHED_EVENT).defaultFailureEvent()
            .from(REBOOT_WAIT_UNTIL_AVAILABLE_STATE).to(REBOOT_FINISHED_STATE)
            .event(REBOOT_WAIT_UNTIL_AVAILABLE_FINISHED_EVENT).failureEvent(REBOOT_WAIT_UNTIL_AVAILABLE_FAILURE_EVENT)
            .from(REBOOT_FINISHED_STATE).to(FINAL_STATE).event(REBOOT_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<RebootState, RebootEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, REBOOT_FAILED_STATE, REBOOT_FAIL_HANDLED_EVENT);

    public RebootFlowConfig() {
        super(RebootState.class, RebootEvent.class);
    }

    @Override
    public RebootEvent[] getEvents() {
        return RebootEvent.values();
    }

    @Override
    public RebootEvent[] getInitEvents() {
        return new RebootEvent[] {
                REBOOT_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Reboot instance";
    }

    @Override
    protected List<Transition<RebootState, RebootEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<RebootState, RebootEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

}

