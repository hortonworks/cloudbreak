package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DetermineDatalakeDataSizesFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<DetermineDatalakeDataSizesState, DetermineDatalakeDataSizesEvent>
        implements RetryableFlowConfiguration<DetermineDatalakeDataSizesEvent> {
    private static final List<Transition<DetermineDatalakeDataSizesState, DetermineDatalakeDataSizesEvent>> TRANSITIONS =
            new Builder<DetermineDatalakeDataSizesState, DetermineDatalakeDataSizesEvent>()
                    .defaultFailureEvent(DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_FAILURE_EVENT)

                    .from(DetermineDatalakeDataSizesState.INIT_STATE)
                    .to(DetermineDatalakeDataSizesState.DETERMINE_DATALAKE_DATA_SIZES_SALT_UPDATE_STATE)
                    .event(DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_EVENT).noFailureEvent()

                    .from(DetermineDatalakeDataSizesState.DETERMINE_DATALAKE_DATA_SIZES_SALT_UPDATE_STATE)
                    .to(DetermineDatalakeDataSizesState.DETERMINE_DATALAKE_DATA_SIZES_IN_PROGRESS_STATE)
                    .event(DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_SALT_UPDATE_FINISHED_EVENT).defaultFailureEvent()

                    .from(DetermineDatalakeDataSizesState.DETERMINE_DATALAKE_DATA_SIZES_IN_PROGRESS_STATE)
                    .to(DetermineDatalakeDataSizesState.DETERMINE_DATALAKE_DATA_SIZES_SUBMISSION_STATE)
                    .event(DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_SUBMISSION_EVENT).defaultFailureEvent()

                    .from(DetermineDatalakeDataSizesState.DETERMINE_DATALAKE_DATA_SIZES_SUBMISSION_STATE)
                    .to(DetermineDatalakeDataSizesState.FINAL_STATE)
                    .event(DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_SUBMISSION_SUCCESS_EVENT).defaultFailureEvent()

                    .from(DetermineDatalakeDataSizesState.DETERMINE_DATALAKE_DATA_SIZES_FAILURE_STATE)
                    .to(DetermineDatalakeDataSizesState.FINAL_STATE)
                    .event(DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_FAILURE_HANDLED_EVENT).noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DetermineDatalakeDataSizesState, DetermineDatalakeDataSizesEvent> EDGE_CONFIG = new FlowEdgeConfig<>(
            DetermineDatalakeDataSizesState.INIT_STATE, DetermineDatalakeDataSizesState.FINAL_STATE,
            DetermineDatalakeDataSizesState.DETERMINE_DATALAKE_DATA_SIZES_FAILURE_STATE,
            DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_FAILURE_HANDLED_EVENT
    );

    public DetermineDatalakeDataSizesFlowConfig() {
        super(DetermineDatalakeDataSizesState.class, DetermineDatalakeDataSizesEvent.class);
    }

    @Override
    public DetermineDatalakeDataSizesEvent[] getEvents() {
        return DetermineDatalakeDataSizesEvent.values();
    }

    @Override
    public DetermineDatalakeDataSizesEvent[] getInitEvents() {
        return new DetermineDatalakeDataSizesEvent[] {DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Determine datalake data sizes flow";
    }

    @Override
    public DetermineDatalakeDataSizesEvent getRetryableEvent() {
        return DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_FAILURE_HANDLED_EVENT;
    }

    @Override
    protected List<Transition<DetermineDatalakeDataSizesState, DetermineDatalakeDataSizesEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<DetermineDatalakeDataSizesState, DetermineDatalakeDataSizesEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
