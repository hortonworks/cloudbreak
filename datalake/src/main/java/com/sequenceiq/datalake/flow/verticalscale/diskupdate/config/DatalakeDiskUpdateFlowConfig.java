package com.sequenceiq.datalake.flow.verticalscale.diskupdate.config;

import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.DatalakeDiskUpdateState.DATALAKE_DISK_UPDATE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.DatalakeDiskUpdateState.DATALAKE_DISK_UPDATE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.DatalakeDiskUpdateState.DATALAKE_DISK_UPDATE_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.DatalakeDiskUpdateState.DATALAKE_DISK_UPDATE_VALIDATION_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.DatalakeDiskUpdateState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.DatalakeDiskUpdateState.INIT_STATE;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_FINALIZE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_FINISH_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.FAILED_DATALAKE_DISK_UPDATE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.HANDLED_FAILED_DATALAKE_DISK_UPDATE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.DatalakeDiskUpdateState;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class DatalakeDiskUpdateFlowConfig extends AbstractFlowConfiguration<DatalakeDiskUpdateState, DatalakeDiskUpdateStateSelectors>
        implements RetryableDatalakeFlowConfiguration<DatalakeDiskUpdateStateSelectors> {

    private static final List<Transition<DatalakeDiskUpdateState, DatalakeDiskUpdateStateSelectors>> TRANSITIONS =
            new Transition.Builder<DatalakeDiskUpdateState, DatalakeDiskUpdateStateSelectors>()
            .defaultFailureEvent(FAILED_DATALAKE_DISK_UPDATE_EVENT)

            .from(INIT_STATE)
            .to(DATALAKE_DISK_UPDATE_VALIDATION_STATE)
            .event(DATALAKE_DISK_UPDATE_VALIDATION_EVENT)
            .defaultFailureEvent()

            .from(DATALAKE_DISK_UPDATE_VALIDATION_STATE)
            .to(DATALAKE_DISK_UPDATE_STATE)
            .event(DATALAKE_DISK_UPDATE_EVENT)
            .defaultFailureEvent()

            .from(DATALAKE_DISK_UPDATE_STATE)
            .to(DATALAKE_DISK_UPDATE_FINISHED_STATE)
            .event(DATALAKE_DISK_UPDATE_FINISH_EVENT)
            .defaultFailureEvent()

            .from(DATALAKE_DISK_UPDATE_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(DATALAKE_DISK_UPDATE_FINALIZE_EVENT)
            .defaultFailureEvent()

            .build();

    protected DatalakeDiskUpdateFlowConfig() {
        super(DatalakeDiskUpdateState.class, DatalakeDiskUpdateStateSelectors.class);
    }

    @Override
    protected List<Transition<DatalakeDiskUpdateState, DatalakeDiskUpdateStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeDiskUpdateState, DatalakeDiskUpdateStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                INIT_STATE,
                FINAL_STATE,
                DATALAKE_DISK_UPDATE_FAILED_STATE,
                HANDLED_FAILED_DATALAKE_DISK_UPDATE_EVENT
        );
    }

    @Override
    public DatalakeDiskUpdateStateSelectors[] getEvents() {
        return DatalakeDiskUpdateStateSelectors.values();
    }

    @Override
    public DatalakeDiskUpdateStateSelectors[] getInitEvents() {
        return new DatalakeDiskUpdateStateSelectors[] {
                DATALAKE_DISK_UPDATE_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Data Lake Disk Resize and Type Update";
    }

    @Override
    public DatalakeDiskUpdateStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_DATALAKE_DISK_UPDATE_EVENT;
    }
}
