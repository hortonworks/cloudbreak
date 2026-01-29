package com.sequenceiq.datalake.flow.start;

import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_RDS_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.start.SdxStartState.INIT_STATE;
import static com.sequenceiq.datalake.flow.start.SdxStartState.SDX_START_FAILED_STATE;
import static com.sequenceiq.datalake.flow.start.SdxStartState.SDX_START_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.start.SdxStartState.SDX_START_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.start.SdxStartState.SDX_START_RDS_START_STATE;
import static com.sequenceiq.datalake.flow.start.SdxStartState.SDX_START_START_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class SdxStartFlowConfig extends AbstractFlowConfiguration<SdxStartState, SdxStartEvent>
        implements RetryableDatalakeFlowConfiguration<SdxStartEvent> {

    private static final List<Transition<SdxStartState, SdxStartEvent>> TRANSITIONS = new Transition.Builder<SdxStartState, SdxStartEvent>()
            .defaultFailureEvent(SDX_START_FAILED_EVENT)

            .from(INIT_STATE).to(SDX_START_RDS_START_STATE)
            .event(SDX_START_EVENT).defaultFailureEvent()

            .from(SDX_START_RDS_START_STATE).to(SDX_START_START_STATE)
            .event(SDX_START_RDS_FINISHED_EVENT).defaultFailureEvent()

            .from(SDX_START_START_STATE).to(SDX_START_IN_PROGRESS_STATE)
            .event(SDX_START_IN_PROGRESS_EVENT).defaultFailureEvent()

            .from(SDX_START_IN_PROGRESS_STATE).to(SDX_START_FINISHED_STATE)
            .event(SDX_START_SUCCESS_EVENT).failureEvent(SDX_START_FAILED_EVENT)

            .from(SDX_START_FINISHED_STATE).to(FINAL_STATE)
            .event(SDX_START_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<SdxStartState, SdxStartEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SDX_START_FAILED_STATE, SDX_START_FAILED_HANDLED_EVENT);

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
                SDX_START_EVENT
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
    public FlowEdgeConfig<SdxStartState, SdxStartEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SdxStartEvent getRetryableEvent() {
        return SDX_START_FAILED_HANDLED_EVENT;
    }

    @Override
    public List<SdxStartEvent> getStackRetryEvents() {
        return List.of(SDX_START_IN_PROGRESS_EVENT);
    }
}
