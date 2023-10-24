package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateState.DATAHUB_DISK_UPDATE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateState.DATAHUB_DISK_UPDATE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateState.DATAHUB_DISK_UPDATE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateState.DATAHUB_DISK_UPDATE_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateState.DISK_RESIZE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_RESIZE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_FINALIZE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_FINISH_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.FAILED_DATAHUB_DISK_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.HANDLED_FAILED_DATAHUB_DISK_UPDATE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DistroXDiskUpdateFlowConfig extends StackStatusFinalizerAbstractFlowConfig<DistroXDiskUpdateState, DistroXDiskUpdateStateSelectors>
        implements RetryableFlowConfiguration<DistroXDiskUpdateStateSelectors> {

    private static final List<Transition<DistroXDiskUpdateState, DistroXDiskUpdateStateSelectors>> TRANSITIONS =
            new Transition.Builder<DistroXDiskUpdateState, DistroXDiskUpdateStateSelectors>()
                    .defaultFailureEvent(FAILED_DATAHUB_DISK_UPDATE_EVENT)

                    .from(INIT_STATE)
                    .to(DATAHUB_DISK_UPDATE_VALIDATION_STATE)
                    .event(DATAHUB_DISK_UPDATE_VALIDATION_EVENT)
                    .defaultFailureEvent()

                    .from(DATAHUB_DISK_UPDATE_VALIDATION_STATE)
                    .to(DATAHUB_DISK_UPDATE_STATE)
                    .event(DATAHUB_DISK_UPDATE_EVENT)
                    .defaultFailureEvent()

                    .from(DATAHUB_DISK_UPDATE_STATE)
                    .to(DISK_RESIZE_STATE)
                    .event(DATAHUB_DISK_UPDATE_FINISH_EVENT)
                    .defaultFailureEvent()

                    .from(DISK_RESIZE_STATE)
                    .to(DATAHUB_DISK_UPDATE_FINISHED_STATE)
                    .event(DATAHUB_DISK_RESIZE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(DATAHUB_DISK_UPDATE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DATAHUB_DISK_UPDATE_FINALIZE_EVENT)
                    .defaultFailureEvent()

                    .build();

    protected DistroXDiskUpdateFlowConfig() {
        super(DistroXDiskUpdateState.class, DistroXDiskUpdateStateSelectors.class);
    }

    @Override
    protected List<Transition<DistroXDiskUpdateState, DistroXDiskUpdateStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<DistroXDiskUpdateState, DistroXDiskUpdateStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                INIT_STATE,
                FINAL_STATE,
                DATAHUB_DISK_UPDATE_FAILED_STATE,
                HANDLED_FAILED_DATAHUB_DISK_UPDATE_EVENT
        );
    }

    @Override
    public DistroXDiskUpdateStateSelectors[] getEvents() {
        return DistroXDiskUpdateStateSelectors.values();
    }

    @Override
    public DistroXDiskUpdateStateSelectors[] getInitEvents() {
        return new DistroXDiskUpdateStateSelectors[] {
                DATAHUB_DISK_UPDATE_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Vertical Scale Resizing and changing disk type - Data Hub";
    }

    @Override
    public DistroXDiskUpdateStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_DATAHUB_DISK_UPDATE_EVENT;
    }

}
