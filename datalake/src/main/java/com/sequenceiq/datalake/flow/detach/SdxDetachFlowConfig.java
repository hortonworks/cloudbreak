package com.sequenceiq.datalake.flow.detach;

import static com.sequenceiq.datalake.flow.detach.SdxDetachState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.INIT_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.SDX_DETACH_FAILED_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.SDX_DETACH_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.SDX_DETACH_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.SDX_DETACH_START_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class SdxDetachFlowConfig extends AbstractFlowConfiguration<SdxDetachState, SdxDetachEvent> implements RetryableFlowConfiguration<SdxDetachEvent> {

    private static final List<Transition<SdxDetachState, SdxDetachEvent>> TRANSITIONS = new Transition.Builder<SdxDetachState, SdxDetachEvent>()
            .defaultFailureEvent(SdxDetachEvent.SDX_DETACH_FAILED_EVENT)

            .from(INIT_STATE).to(SDX_DETACH_START_STATE)
            .event(SdxDetachEvent.SDX_DETACH_EVENT).noFailureEvent()

            .from(SDX_DETACH_START_STATE).to(SDX_DETACH_IN_PROGRESS_STATE)
            .event(SdxDetachEvent.SDX_DETACH_IN_PROGRESS_EVENT).failureEvent(SdxDetachEvent.SDX_DETACH_FAILED_EVENT)

            .from(SDX_DETACH_IN_PROGRESS_STATE).to(SDX_DETACH_FINISHED_STATE)
            .event(SdxDetachEvent.SDX_DETACH_SUCCESS_EVENT).defaultFailureEvent()

            .from(SDX_DETACH_FINISHED_STATE).to(FINAL_STATE)
            .event(SdxDetachEvent.SDX_DETACH_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<SdxDetachState, SdxDetachEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SDX_DETACH_FAILED_STATE, SdxDetachEvent.SDX_DETACH_FAILED_HANDLED_EVENT);

    public SdxDetachFlowConfig() {
        super(SdxDetachState.class, SdxDetachEvent.class);
    }

    @Override
    public SdxDetachEvent[] getEvents() {
        return SdxDetachEvent.values();
    }

    @Override
    public SdxDetachEvent[] getInitEvents() {
        return new SdxDetachEvent[]{
                SdxDetachEvent.SDX_DETACH_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Sdx stop";
    }

    @Override
    protected List<Transition<SdxDetachState, SdxDetachEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<SdxDetachState, SdxDetachEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SdxDetachEvent getRetryableEvent() {
        return SdxDetachEvent.SDX_DETACH_FAILED_HANDLED_EVENT;
    }
}
