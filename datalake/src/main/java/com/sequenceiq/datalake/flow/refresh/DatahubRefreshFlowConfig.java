package com.sequenceiq.datalake.flow.refresh;

import static com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowEvent.DATAHUB_REFRESH_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowEvent.DATAHUB_REFRESH_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowEvent.DATAHUB_REFRESH_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowEvent.DATAHUB_REFRESH_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowEvent.DATAHUB_REFRESH_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowEvent.DATAHUB_REFRESH_START_EVENT;
import static com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowState.DATAHUB_REFRESH_FAILED_STATE;
import static com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowState.DATAHUB_REFRESH_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowState.DATAHUB_REFRESH_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowState.DATAHUB_REFRESH_START_STATE;
import static com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class DatahubRefreshFlowConfig extends AbstractFlowConfiguration<DatahubRefreshFlowState, DatahubRefreshFlowEvent>
        implements RetryableDatalakeFlowConfiguration<DatahubRefreshFlowEvent> {

    private static final List<Transition<DatahubRefreshFlowState, DatahubRefreshFlowEvent>> TRANSITIONS
            = new Builder<DatahubRefreshFlowState, DatahubRefreshFlowEvent>()
            .defaultFailureEvent(DATAHUB_REFRESH_FAILED_EVENT)

            .from(INIT_STATE)
            .to(DATAHUB_REFRESH_START_STATE)
            .event(DATAHUB_REFRESH_START_EVENT)
            .defaultFailureEvent()

            .from(DATAHUB_REFRESH_START_STATE)
            .to(DATAHUB_REFRESH_IN_PROGRESS_STATE)
            .event(DATAHUB_REFRESH_IN_PROGRESS_EVENT)
            .defaultFailureEvent()

            .from(DATAHUB_REFRESH_IN_PROGRESS_STATE)
            .to(DATAHUB_REFRESH_FINISHED_STATE)
            .event(DATAHUB_REFRESH_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(DATAHUB_REFRESH_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(DATAHUB_REFRESH_FINALIZED_EVENT)
            .defaultFailureEvent()
            .build();

    protected DatahubRefreshFlowConfig() {
        super(DatahubRefreshFlowState.class, DatahubRefreshFlowEvent.class);
    }

    @Override
    protected List<Transition<DatahubRefreshFlowState, DatahubRefreshFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatahubRefreshFlowState, DatahubRefreshFlowEvent> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATAHUB_REFRESH_FAILED_STATE, DATAHUB_REFRESH_FAILED_HANDLED_EVENT);
    }

    @Override
    public DatahubRefreshFlowEvent[] getEvents() {
        return DatahubRefreshFlowEvent.values();
    }

    @Override
    public DatahubRefreshFlowEvent[] getInitEvents() {
        return new DatahubRefreshFlowEvent[]{DATAHUB_REFRESH_START_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Refresh Datahub Configuration";
    }

    @Override
    public DatahubRefreshFlowEvent getRetryableEvent() {
        return DATAHUB_REFRESH_FAILED_EVENT;
    }
}
