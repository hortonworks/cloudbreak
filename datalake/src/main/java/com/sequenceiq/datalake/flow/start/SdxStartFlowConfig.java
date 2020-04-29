package com.sequenceiq.datalake.flow.start;

import static com.sequenceiq.datalake.flow.start.SdxStartState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.start.SdxStartState.INIT_STATE;
import static com.sequenceiq.datalake.flow.start.SdxStartState.SDX_START_FAILED_STATE;
import static com.sequenceiq.datalake.flow.start.SdxStartState.SDX_START_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.start.SdxStartState.SDX_START_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.start.SdxStartState.SDX_START_START_STATE;
import static com.sequenceiq.datalake.flow.start.SdxStartState.SDX_START_SYNC_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class SdxStartFlowConfig extends AbstractFlowConfiguration<SdxStartState, SdxStartEvent> implements RetryableFlowConfiguration<SdxStartEvent> {

    private static final List<Transition<SdxStartState, SdxStartEvent>> TRANSITIONS = new Transition.Builder<SdxStartState, SdxStartEvent>()
            .defaultFailureEvent(SdxStartEvent.SDX_START_FAILED_EVENT)

            .from(INIT_STATE).to(SDX_START_SYNC_STATE)
            .event(SdxStartEvent.SDX_START_EVENT).noFailureEvent()

            .from(SDX_START_SYNC_STATE).to(SDX_START_START_STATE)
            .event(SdxStartEvent.SDX_SYNC_FINISHED_EVENT).noFailureEvent()

            .from(SDX_START_START_STATE).to(SDX_START_IN_PROGRESS_STATE)
            .event(SdxStartEvent.SDX_START_IN_PROGRESS_EVENT).defaultFailureEvent()

            .from(SDX_START_IN_PROGRESS_STATE).to(SDX_START_FINISHED_STATE)
            .event(SdxStartEvent.SDX_START_SUCCESS_EVENT).failureEvent(SdxStartEvent.SDX_START_FAILED_EVENT)

            .from(SDX_START_FINISHED_STATE).to(FINAL_STATE)
            .event(SdxStartEvent.SDX_START_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<SdxStartState, SdxStartEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SDX_START_FAILED_STATE, SdxStartEvent.SDX_START_FAILED_HANDLED_EVENT);

    public SdxStartFlowConfig() {
        super(SdxStartState.class, SdxStartEvent.class);
    }

    @Override
    public SdxStartEvent[] getEvents() {
        return SdxStartEvent.values();
    }

    @Override
    public SdxStartEvent[] getInitEvents() {
        return new SdxStartEvent[]{
                SdxStartEvent.SDX_START_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Sdx start";
    }

    @Override
    protected List<Transition<SdxStartState, SdxStartEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<SdxStartState, SdxStartEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SdxStartEvent getRetryableEvent() {
        return SdxStartEvent.SDX_START_FAILED_HANDLED_EVENT;
    }
}
