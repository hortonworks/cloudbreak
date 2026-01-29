package com.sequenceiq.datalake.flow.datahub;

import static com.sequenceiq.datalake.flow.datahub.StartDatahubFlowEvent.START_DATAHUB_EVENT;
import static com.sequenceiq.datalake.flow.datahub.StartDatahubFlowEvent.START_DATAHUB_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.datahub.StartDatahubFlowEvent.START_DATAHUB_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.datahub.StartDatahubFlowState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.datahub.StartDatahubFlowState.INIT_STATE;
import static com.sequenceiq.datalake.flow.datahub.StartDatahubFlowState.START_DATAHUB_FAILED_STATE;
import static com.sequenceiq.datalake.flow.datahub.StartDatahubFlowState.START_DATAHUB_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.datahub.StartDatahubFlowState.START_DATAHUB_STATE;

import java.util.List;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

public class StartDatahubFlowConfig extends AbstractFlowConfiguration<StartDatahubFlowState, StartDatahubFlowEvent>
        implements RetryableDatalakeFlowConfiguration<StartDatahubFlowEvent> {

    private static final List<Transition<StartDatahubFlowState, StartDatahubFlowEvent>> TRANSITIONS =
            new Transition.Builder<StartDatahubFlowState, StartDatahubFlowEvent>()
                    .defaultFailureEvent(StartDatahubFlowEvent.START_DATAHUB_FAILED_EVENT)

                    .from(INIT_STATE).to(START_DATAHUB_STATE)
                    .event(START_DATAHUB_EVENT)
                    .defaultFailureEvent()

                    .from(START_DATAHUB_STATE).to(START_DATAHUB_FINISHED_STATE)
                    .event(START_DATAHUB_IN_PROGRESS_EVENT).defaultFailureEvent()

                    .from(START_DATAHUB_FINISHED_STATE).to(FINAL_STATE)
                    .event(START_DATAHUB_SUCCESS_EVENT).defaultFailureEvent()

                    .build();

    protected StartDatahubFlowConfig() {
        super(StartDatahubFlowState.class, StartDatahubFlowEvent.class);
    }

    @Override
    protected List<Transition<StartDatahubFlowState, StartDatahubFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<StartDatahubFlowState, StartDatahubFlowEvent> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, START_DATAHUB_FAILED_STATE, StartDatahubFlowEvent.START_DATAHUB_HANDLED_EVENT);
    }

    @Override
    public StartDatahubFlowEvent[] getEvents() {
        return StartDatahubFlowEvent.values();
    }

    @Override
    public StartDatahubFlowEvent[] getInitEvents() {
        return new StartDatahubFlowEvent[]{START_DATAHUB_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "start datahub cluster flow";
    }

    @Override
    public StartDatahubFlowEvent getRetryableEvent() {
        return null;
    }
}

