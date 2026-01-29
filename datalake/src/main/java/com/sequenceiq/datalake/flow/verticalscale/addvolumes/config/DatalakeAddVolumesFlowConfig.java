package com.sequenceiq.datalake.flow.verticalscale.addvolumes.config;

import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.DatalakeAddVolumesState.DATALAKE_ADD_VOLUMES_FAILED_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.DatalakeAddVolumesState.DATALAKE_ADD_VOLUMES_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.DatalakeAddVolumesState.DATALAKE_ADD_VOLUMES_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.DatalakeAddVolumesState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.DatalakeAddVolumesState.INIT_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.DATALAKE_ADD_VOLUMES_FINALIZE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.DATALAKE_ADD_VOLUMES_FINISH_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.DATALAKE_ADD_VOLUMES_TRIGGER_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.FAILED_DATALAKE_ADD_VOLUMES_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.HANDLED_FAILED_DATALAKE_ADD_VOLUMES_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.datalake.flow.verticalscale.addvolumes.DatalakeAddVolumesState;
import com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class DatalakeAddVolumesFlowConfig extends AbstractFlowConfiguration<DatalakeAddVolumesState, DatalakeAddVolumesStateSelectors>
        implements RetryableDatalakeFlowConfiguration<DatalakeAddVolumesStateSelectors> {

    private static final List<Transition<DatalakeAddVolumesState, DatalakeAddVolumesStateSelectors>> TRANSITIONS =
            new AbstractFlowConfiguration.Transition.Builder<DatalakeAddVolumesState, DatalakeAddVolumesStateSelectors>()
                    .defaultFailureEvent(FAILED_DATALAKE_ADD_VOLUMES_EVENT)

                    .from(INIT_STATE)
                    .to(DATALAKE_ADD_VOLUMES_STATE)
                    .event(DATALAKE_ADD_VOLUMES_TRIGGER_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_ADD_VOLUMES_STATE)
                    .to(DATALAKE_ADD_VOLUMES_FINISHED_STATE)
                    .event(DATALAKE_ADD_VOLUMES_FINISH_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_ADD_VOLUMES_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_ADD_VOLUMES_FINALIZE_EVENT)
                    .defaultFailureEvent()

                    .build();

    protected DatalakeAddVolumesFlowConfig() {
        super(DatalakeAddVolumesState.class, DatalakeAddVolumesStateSelectors.class);
    }

    @Override
    protected List<Transition<DatalakeAddVolumesState, DatalakeAddVolumesStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeAddVolumesState, DatalakeAddVolumesStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                INIT_STATE,
                FINAL_STATE,
                DATALAKE_ADD_VOLUMES_FAILED_STATE,
                HANDLED_FAILED_DATALAKE_ADD_VOLUMES_EVENT
        );
    }

    @Override
    public DatalakeAddVolumesStateSelectors[] getEvents() {
        return DatalakeAddVolumesStateSelectors.values();
    }

    @Override
    public DatalakeAddVolumesStateSelectors[] getInitEvents() {
        return new DatalakeAddVolumesStateSelectors[] {
                DATALAKE_ADD_VOLUMES_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Data Lake Add Volumes";
    }

    @Override
    public DatalakeAddVolumesStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_DATALAKE_ADD_VOLUMES_EVENT;
    }
}