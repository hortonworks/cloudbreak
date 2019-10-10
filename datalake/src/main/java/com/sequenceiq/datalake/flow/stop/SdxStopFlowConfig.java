package com.sequenceiq.datalake.flow.stop;

import static com.sequenceiq.datalake.flow.stop.SdxStopState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.stop.SdxStopState.INIT_STATE;
import static com.sequenceiq.datalake.flow.stop.SdxStopState.SDX_STOP_FAILED_STATE;
import static com.sequenceiq.datalake.flow.stop.SdxStopState.SDX_STOP_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.stop.SdxStopState.SDX_STOP_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.stop.SdxStopState.SDX_STOP_START_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class SdxStopFlowConfig extends AbstractFlowConfiguration<SdxStopState, SdxStopEvent> {

    private static final List<Transition<SdxStopState, SdxStopEvent>> TRANSITIONS = new Transition.Builder<SdxStopState, SdxStopEvent>()
            .defaultFailureEvent(SdxStopEvent.SDX_STOP_FAILED_EVENT)
            .from(INIT_STATE).to(SDX_STOP_START_STATE)
            .event(SdxStopEvent.SDX_STOP_EVENT).noFailureEvent()

            .from(SDX_STOP_START_STATE).to(SDX_STOP_IN_PROGRESS_STATE)
            .event(SdxStopEvent.SDX_STOP_IN_PROGRESS_EVENT).defaultFailureEvent()

            .from(SDX_STOP_IN_PROGRESS_STATE).to(SDX_STOP_FINISHED_STATE)
            .event(SdxStopEvent.SDX_STOP_SUCCESS_EVENT).failureEvent(SdxStopEvent.SDX_STOP_FAILED_EVENT)

            .from(SDX_STOP_FINISHED_STATE).to(FINAL_STATE)
            .event(SdxStopEvent.SDX_STOP_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<SdxStopState, SdxStopEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SDX_STOP_FAILED_STATE, SdxStopEvent.SDX_STOP_FAILED_HANDLED_EVENT);

    public SdxStopFlowConfig() {
        super(SdxStopState.class, SdxStopEvent.class);
    }

    @Override
    public SdxStopEvent[] getEvents() {
        return SdxStopEvent.values();
    }

    @Override
    public SdxStopEvent[] getInitEvents() {
        return new SdxStopEvent[]{
                SdxStopEvent.SDX_STOP_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Sdx stop";
    }

    @Override
    protected List<Transition<SdxStopState, SdxStopEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<SdxStopState, SdxStopEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
