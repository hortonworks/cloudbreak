package com.sequenceiq.cloudbreak.core.flow2.cluster.restart;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.restart.RestartEvent.RESTART_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restart.RestartEvent.RESTART_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restart.RestartEvent.RESTART_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restart.RestartEvent.RESTART_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restart.RestartEvent.RESTART_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restart.RestartState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restart.RestartState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restart.RestartState.RESTART_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restart.RestartState.RESTART_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restart.RestartState.RESTART_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;

@Component
public class RestartFlowConfig extends StackStatusFinalizerAbstractFlowConfig<RestartState, RestartEvent> {

    private static final List<Transition<RestartState, RestartEvent>> TRANSITIONS = new Transition.Builder<RestartState, RestartEvent>()
            .defaultFailureEvent(RESTART_FAILURE_EVENT)
            .from(INIT_STATE).to(RESTART_STATE).event(RESTART_TRIGGER_EVENT).defaultFailureEvent()
            .from(RESTART_STATE).to(RESTART_FINISHED_STATE).event(RESTART_FINISHED_EVENT).defaultFailureEvent()
            .from(RESTART_FINISHED_STATE).to(FINAL_STATE).event(RESTART_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<RestartState, RestartEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, RESTART_FAILED_STATE, RESTART_FAIL_HANDLED_EVENT);

    public RestartFlowConfig() {
        super(RestartState.class, RestartEvent.class);
    }

    @Override
    public RestartEvent[] getEvents() {
        return RestartEvent.values();
    }

    @Override
    public RestartEvent[] getInitEvents() {
        return new RestartEvent[] {
                RESTART_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Restart instance";
    }

    @Override
    protected List<Transition<RestartState, RestartEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<RestartState, RestartEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
