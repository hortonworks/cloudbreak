package com.sequenceiq.datalake.flow.verticalscale.rootvolume.config;

import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.DatalakeRootVolumeUpdateState.DATALAKE_ROOT_VOLUME_UPDATE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.DatalakeRootVolumeUpdateState.DATALAKE_ROOT_VOLUME_UPDATE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.DatalakeRootVolumeUpdateState.DATALAKE_ROOT_VOLUME_UPDATE_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.DatalakeRootVolumeUpdateState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.DatalakeRootVolumeUpdateState.INIT_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.DATALAKE_ROOT_VOLUME_UPDATE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.DATALAKE_ROOT_VOLUME_UPDATE_FINALIZE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.DATALAKE_ROOT_VOLUME_UPDATE_FINISH_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.HANDLED_FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.datalake.flow.verticalscale.rootvolume.DatalakeRootVolumeUpdateState;
import com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class DatalakeRootVolumeUpdateFlowConfig extends AbstractFlowConfiguration<DatalakeRootVolumeUpdateState, DatalakeRootVolumeUpdateStateSelectors>
        implements RetryableDatalakeFlowConfiguration<DatalakeRootVolumeUpdateStateSelectors> {

    private static final List<Transition<DatalakeRootVolumeUpdateState, DatalakeRootVolumeUpdateStateSelectors>> TRANSITIONS =
            new Transition.Builder<DatalakeRootVolumeUpdateState, DatalakeRootVolumeUpdateStateSelectors>()
            .defaultFailureEvent(FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT)

            .from(INIT_STATE)
            .to(DATALAKE_ROOT_VOLUME_UPDATE_STATE)
            .event(DATALAKE_ROOT_VOLUME_UPDATE_EVENT)
            .defaultFailureEvent()

            .from(DATALAKE_ROOT_VOLUME_UPDATE_STATE)
            .to(DATALAKE_ROOT_VOLUME_UPDATE_FINISHED_STATE)
            .event(DATALAKE_ROOT_VOLUME_UPDATE_FINISH_EVENT)
            .defaultFailureEvent()

            .from(DATALAKE_ROOT_VOLUME_UPDATE_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(DATALAKE_ROOT_VOLUME_UPDATE_FINALIZE_EVENT)
            .defaultFailureEvent()

            .build();

    protected DatalakeRootVolumeUpdateFlowConfig() {
        super(DatalakeRootVolumeUpdateState.class, DatalakeRootVolumeUpdateStateSelectors.class);
    }

    @Override
    protected List<Transition<DatalakeRootVolumeUpdateState, DatalakeRootVolumeUpdateStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeRootVolumeUpdateState, DatalakeRootVolumeUpdateStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                INIT_STATE,
                FINAL_STATE,
                DATALAKE_ROOT_VOLUME_UPDATE_FAILED_STATE,
                HANDLED_FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT
        );
    }

    @Override
    public DatalakeRootVolumeUpdateStateSelectors[] getEvents() {
        return DatalakeRootVolumeUpdateStateSelectors.values();
    }

    @Override
    public DatalakeRootVolumeUpdateStateSelectors[] getInitEvents() {
        return new DatalakeRootVolumeUpdateStateSelectors[] {
                DATALAKE_ROOT_VOLUME_UPDATE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Data Lake Root Disk Resize and Type Update";
    }

    @Override
    public DatalakeRootVolumeUpdateStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT;
    }
}
