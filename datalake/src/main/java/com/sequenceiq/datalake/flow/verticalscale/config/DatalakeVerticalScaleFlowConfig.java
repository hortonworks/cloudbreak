package com.sequenceiq.datalake.flow.verticalscale.config;

import static com.sequenceiq.datalake.flow.verticalscale.DatalakeVerticalScaleState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.DatalakeVerticalScaleState.INIT_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.DatalakeVerticalScaleState.VERTICAL_SCALING_DATALAKE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.DatalakeVerticalScaleState.VERTICAL_SCALING_DATALAKE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.DatalakeVerticalScaleState.VERTICAL_SCALING_DATALAKE_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.DatalakeVerticalScaleState.VERTICAL_SCALING_DATALAKE_VALIDATION_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleStateSelectors.FAILED_VERTICAL_SCALING_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleStateSelectors.FINALIZE_VERTICAL_SCALING_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleStateSelectors.FINISH_VERTICAL_SCALING_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleStateSelectors.HANDLED_FAILED_VERTICAL_SCALING_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleStateSelectors.VERTICAL_SCALING_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleStateSelectors.VERTICAL_SCALING_DATALAKE_VALIDATION_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.datalake.flow.verticalscale.DatalakeVerticalScaleState;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class DatalakeVerticalScaleFlowConfig extends AbstractFlowConfiguration<DatalakeVerticalScaleState, DatalakeVerticalScaleStateSelectors>
        implements RetryableDatalakeFlowConfiguration<DatalakeVerticalScaleStateSelectors> {

    private static final List<Transition<DatalakeVerticalScaleState, DatalakeVerticalScaleStateSelectors>> TRANSITIONS =
            new Transition.Builder<DatalakeVerticalScaleState, DatalakeVerticalScaleStateSelectors>()
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

    protected DatalakeVerticalScaleFlowConfig() {
        super(DatalakeVerticalScaleState.class, DatalakeVerticalScaleStateSelectors.class);
    }

    @Override
    protected List<Transition<DatalakeVerticalScaleState, DatalakeVerticalScaleStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeVerticalScaleState, DatalakeVerticalScaleStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                INIT_STATE,
                FINAL_STATE,
                VERTICAL_SCALING_DATALAKE_FAILED_STATE,
                HANDLED_FAILED_VERTICAL_SCALING_DATALAKE_EVENT
        );
    }

    @Override
    public DatalakeVerticalScaleStateSelectors[] getEvents() {
        return DatalakeVerticalScaleStateSelectors.values();
    }

    @Override
    public DatalakeVerticalScaleStateSelectors[] getInitEvents() {
        return new DatalakeVerticalScaleStateSelectors[] {
                VERTICAL_SCALING_DATALAKE_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Vertical Scale Data Lake";
    }

    @Override
    public DatalakeVerticalScaleStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_VERTICAL_SCALING_DATALAKE_EVENT;
    }
}
