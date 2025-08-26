package com.sequenceiq.datalake.flow.datalake.scale.config;

import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_CM_RESTART_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_START_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_WAIT_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleState.DATALAKE_HORIZONTAL_SCALE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleState.DATALAKE_HORIZONTAL_SCALE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleState.DATALAKE_HORIZONTAL_SCALE_SERVICES_RESTART_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleState.DATALAKE_HORIZONTAL_SCALE_SERVICES_RESTART_STATE;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleState.DATALAKE_HORIZONTAL_SCALE_START_STATE;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleState.DATALAKE_HORIZONTAL_SCALE_VALIDATION_STATE;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleState.DATALAKE_WAIT_FOR_HORIZONTAL_SCALE_STATE;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent;
import com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DatalakeHorizontalScaleFlowConfig extends AbstractFlowConfiguration<DatalakeHorizontalScaleState, DatalakeHorizontalScaleEvent>
        implements RetryableFlowConfiguration<DatalakeHorizontalScaleEvent> {

    private static final List<Transition<DatalakeHorizontalScaleState, DatalakeHorizontalScaleEvent>> TRANSITIONS =
            new Builder<DatalakeHorizontalScaleState, DatalakeHorizontalScaleEvent>()
                    .defaultFailureEvent(DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(DATALAKE_HORIZONTAL_SCALE_VALIDATION_STATE)
                    .event(DATALAKE_HORIZONTAL_SCALE_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_HORIZONTAL_SCALE_VALIDATION_STATE)
                    .to(DATALAKE_HORIZONTAL_SCALE_START_STATE)
                    .event(DATALAKE_HORIZONTAL_SCALE_START_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_HORIZONTAL_SCALE_START_STATE)
                    .to(DATALAKE_WAIT_FOR_HORIZONTAL_SCALE_STATE)
                    .event(DATALAKE_HORIZONTAL_SCALE_WAIT_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_WAIT_FOR_HORIZONTAL_SCALE_STATE)
                    .to(DATALAKE_HORIZONTAL_SCALE_SERVICES_RESTART_STATE)
                    .event(DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_EVENT)
                    .failureEvent(DATALAKE_HORIZONTAL_SCALE_CM_RESTART_FAILED_EVENT)

                    .from(DATALAKE_HORIZONTAL_SCALE_SERVICES_RESTART_STATE)
                    .to(DATALAKE_HORIZONTAL_SCALE_SERVICES_RESTART_IN_PROGRESS_STATE)
                    .event(DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_IN_PROGRESS_EVENT)
                    .failureEvent(DATALAKE_HORIZONTAL_SCALE_CM_RESTART_FAILED_EVENT)

                    .from(DATALAKE_HORIZONTAL_SCALE_SERVICES_RESTART_IN_PROGRESS_STATE)
                    .to(DATALAKE_HORIZONTAL_SCALE_FINISHED_STATE)
                    .event(DATALAKE_HORIZONTAL_SCALE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_HORIZONTAL_SCALE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_HORIZONTAL_SCALE_FINISHED_EVENT)
                    .noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DatalakeHorizontalScaleState, DatalakeHorizontalScaleEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_HORIZONTAL_SCALE_FAILED_STATE,
                    DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT);

    protected DatalakeHorizontalScaleFlowConfig() {
        super(DatalakeHorizontalScaleState.class, DatalakeHorizontalScaleEvent.class);
    }

    @Override
    public DatalakeHorizontalScaleEvent[] getEvents() {
        return DatalakeHorizontalScaleEvent.values();
    }

    @Override
    public DatalakeHorizontalScaleEvent[] getInitEvents() {
        return new DatalakeHorizontalScaleEvent[]{DATALAKE_HORIZONTAL_SCALE_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Horizontal Scale Data Lake";
    }

    @Override
    public List<Transition<DatalakeHorizontalScaleState, DatalakeHorizontalScaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeHorizontalScaleState, DatalakeHorizontalScaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeHorizontalScaleEvent getRetryableEvent() {
        return DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT;
    }

}
