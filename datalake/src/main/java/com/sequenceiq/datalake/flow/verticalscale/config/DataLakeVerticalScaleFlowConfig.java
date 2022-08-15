package com.sequenceiq.datalake.flow.verticalscale.config;

import static com.sequenceiq.datalake.flow.verticalscale.DataLakeVerticalScaleState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.DataLakeVerticalScaleState.INIT_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.DataLakeVerticalScaleState.VERTICAL_SCALING_DATALAKE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.DataLakeVerticalScaleState.VERTICAL_SCALING_DATALAKE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.DataLakeVerticalScaleState.VERTICAL_SCALING_DATALAKE_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.DataLakeVerticalScaleState.VERTICAL_SCALING_DATALAKE_VALIDATION_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleStateSelectors.FAILED_VERTICAL_SCALING_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleStateSelectors.FINALIZE_VERTICAL_SCALING_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleStateSelectors.FINISH_VERTICAL_SCALING_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleStateSelectors.HANDLED_FAILED_VERTICAL_SCALING_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleStateSelectors.VERTICAL_SCALING_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleStateSelectors.VERTICAL_SCALING_DATALAKE_VALIDATION_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.verticalscale.DataLakeVerticalScaleState;
import com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DataLakeVerticalScaleFlowConfig extends AbstractFlowConfiguration<DataLakeVerticalScaleState, DataLakeVerticalScaleStateSelectors>
        implements RetryableFlowConfiguration<DataLakeVerticalScaleStateSelectors> {

    private static final List<Transition<DataLakeVerticalScaleState, DataLakeVerticalScaleStateSelectors>> TRANSITIONS =
            new Transition.Builder<DataLakeVerticalScaleState, DataLakeVerticalScaleStateSelectors>()
            .defaultFailureEvent(FAILED_VERTICAL_SCALING_DATALAKE_EVENT)

            .from(INIT_STATE)
            .to(VERTICAL_SCALING_DATALAKE_VALIDATION_STATE)
            .event(VERTICAL_SCALING_DATALAKE_VALIDATION_EVENT)
            .defaultFailureEvent()

            .from(VERTICAL_SCALING_DATALAKE_VALIDATION_STATE)
            .to(VERTICAL_SCALING_DATALAKE_STATE)
            .event(VERTICAL_SCALING_DATALAKE_EVENT)
            .defaultFailureEvent()

            .from(VERTICAL_SCALING_DATALAKE_STATE)
            .to(VERTICAL_SCALING_DATALAKE_FINISHED_STATE)
            .event(FINISH_VERTICAL_SCALING_DATALAKE_EVENT)
            .defaultFailureEvent()

            .from(VERTICAL_SCALING_DATALAKE_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(FINALIZE_VERTICAL_SCALING_DATALAKE_EVENT)
            .defaultFailureEvent()

            .build();

    protected DataLakeVerticalScaleFlowConfig() {
        super(DataLakeVerticalScaleState.class, DataLakeVerticalScaleStateSelectors.class);
    }

    @Override
    protected List<Transition<DataLakeVerticalScaleState, DataLakeVerticalScaleStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<DataLakeVerticalScaleState, DataLakeVerticalScaleStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                INIT_STATE,
                FINAL_STATE,
                VERTICAL_SCALING_DATALAKE_FAILED_STATE,
                HANDLED_FAILED_VERTICAL_SCALING_DATALAKE_EVENT
        );
    }

    @Override
    public DataLakeVerticalScaleStateSelectors[] getEvents() {
        return DataLakeVerticalScaleStateSelectors.values();
    }

    @Override
    public DataLakeVerticalScaleStateSelectors[] getInitEvents() {
        return new DataLakeVerticalScaleStateSelectors[] {
                VERTICAL_SCALING_DATALAKE_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Vertical Scale Data Lake";
    }

    @Override
    public DataLakeVerticalScaleStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_VERTICAL_SCALING_DATALAKE_EVENT;
    }
}
