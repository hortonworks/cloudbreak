package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.UPDATE_ATTACHED_VOLUMES;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_RESIZE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_UPDATE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_FINALIZE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.HANDLED_FAILED_DATAHUB_DISK_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATAHUB_DISK_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATAHUB_DISK_UPDATE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATAHUB_DISK_UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATAHUB_DISK_UPDATE_VALIDATION_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_RESIZE_STARTED;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskResizeFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Configuration
public class DistroXDiskUpdateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXDiskUpdateActions.class);

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    @Bean(name = "DATAHUB_DISK_UPDATE_VALIDATION_STATE")
    public Action<?, ?> datahubDiskUpdateValidationAction() {
        return new AbstractClusterAction<>(DistroXDiskUpdateEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, DistroXDiskUpdateEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting validations for datahub disk update.");
                Long stackId = payload.getResourceId();
                String targetInstanceGroup = payload.getGroup();
                stackUpdater.updateStackStatus(stackId, UPDATE_ATTACHED_VOLUMES, String.format("Validating volume update on the host " +
                                "group %s", targetInstanceGroup));
                flowMessageService.fireEventAndLog(stackId,
                        Status.DATAHUB_DISK_UPDATE_VALIDATION_IN_PROGRESS.name(),
                        DATAHUB_DISK_UPDATE_VALIDATION_IN_PROGRESS,
                        targetInstanceGroup,
                        payload.getVolumeType(),
                        String.valueOf(payload.getSize()));
                sendEvent(context, DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "DATAHUB_DISK_UPDATE_STATE")
    public Action<?, ?> diskUpdateInDatahubAction() {
        return new AbstractClusterAction<>(DistroXDiskUpdateEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, DistroXDiskUpdateEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting datahub disk update with request {}", payload);
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(
                        stackId,
                        UPDATE_ATTACHED_VOLUMES,
                        String.format("Updating volumes on the host group %s", payload.getGroup()));
                flowMessageService.fireEventAndLog(stackId,
                        UPDATE_IN_PROGRESS.name(),
                        DATAHUB_DISK_UPDATE_IN_PROGRESS,
                        payload.getGroup(),
                        payload.getVolumeType(),
                        String.valueOf(payload.getSize()));
                sendEvent(context, DATAHUB_DISK_UPDATE_HANDLER_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "DISK_RESIZE_STATE")
    public Action<?, ?> diskResizeInDatahubAction() {
        return new AbstractClusterAction<>(DistroXDiskUpdateEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext ctx, DistroXDiskUpdateEvent payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                LOGGER.debug("Starting disk resize for stack: {}", stackId);
                stackUpdater.updateStackStatus(stackId, UPDATE_ATTACHED_VOLUMES, String.format("Resizing volumes on the host group %s",
                        payload.getGroup()));
                flowMessageService.fireEventAndLog(stackId,
                        UPDATE_IN_PROGRESS.name(),
                        DISK_RESIZE_STARTED,
                        payload.getGroup());
                DistroXDiskUpdateEvent handlerRequest = DistroXDiskUpdateEvent.builder()
                        .withResourceId(stackId)
                        .withStackId(payload.getStackId())
                        .withClusterName(payload.getClusterName())
                        .withAccountId(payload.getAccountId())
                        .withDiskType(payload.getDiskType())
                        .withSize(payload.getSize())
                        .withGroup(payload.getGroup())
                        .withVolumeType(payload.getVolumeType())
                        .withVolumesToBeUpdated(payload.getVolumesToBeUpdated())
                        .withSelector(DATAHUB_DISK_RESIZE_HANDLER_EVENT.selector())
                        .build();
                sendEvent(ctx, DATAHUB_DISK_RESIZE_HANDLER_EVENT.event(), handlerRequest);
            }
        };
    }

    @Bean(name = "DATAHUB_DISK_UPDATE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractClusterAction<>(DistroXDiskResizeFinishedEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, DistroXDiskResizeFinishedEvent payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Updating volumes is complete");
                LOGGER.debug("Sending finalize event after disk resize for stack: {}", stackId);
                flowMessageService.fireEventAndLog(stackId,
                            Status.AVAILABLE.name(),
                            DATAHUB_DISK_UPDATE_FINISHED);
                sendEvent(context, DATAHUB_DISK_UPDATE_FINALIZE_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "DATAHUB_DISK_UPDATE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractStackFailureAction<DistroXDiskUpdateState, DistroXDiskUpdateStateSelectors>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Failed Updating volumes");
                LOGGER.warn("Failed to update disks in Datahub for stack {}. Reason: {}.", stackId, payload.getException());
                flowMessageService.fireEventAndLog(stackId,
                        Status.UPDATE_FAILED.name(),
                        DATAHUB_DISK_UPDATE_FAILED,
                        payload.getException().getMessage());
                sendEvent(context, HANDLED_FAILED_DATAHUB_DISK_UPDATE_EVENT.selector(), payload);
            }
        };
    }
}
