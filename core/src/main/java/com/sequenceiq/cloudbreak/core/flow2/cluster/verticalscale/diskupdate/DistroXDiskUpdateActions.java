package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskResizeFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class DistroXDiskUpdateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXDiskUpdateActions.class);

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "DATAHUB_DISK_UPDATE_VALIDATION_STATE")
    public Action<?, ?> datahubDiskUpdateValidationAction() {
        return new AbstractDistroXDiskUpdateAction<>(DistroXDiskUpdateEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DistroXDiskUpdateEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting validations for datahub disk update.");
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        Status.DATAHUB_DISK_UPDATE_VALIDATION_IN_PROGRESS.name(),
                        DATAHUB_DISK_UPDATE_VALIDATION_IN_PROGRESS,
                        payload.getDiskUpdateRequest().getGroup(),
                        payload.getDiskUpdateRequest().getVolumeType(),
                        String.valueOf(payload.getDiskUpdateRequest().getSize()));
                sendEvent(context, DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "DATAHUB_DISK_UPDATE_STATE")
    public Action<?, ?> diskUpdateInDatahubAction() {
        return new AbstractDistroXDiskUpdateAction<>(DistroXDiskUpdateEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DistroXDiskUpdateEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting datahub disk update.");
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        Status.UPDATE_IN_PROGRESS.name(),
                        DATAHUB_DISK_UPDATE_IN_PROGRESS,
                        payload.getDiskUpdateRequest().getGroup(),
                        payload.getDiskUpdateRequest().getVolumeType(),
                        String.valueOf(payload.getDiskUpdateRequest().getSize()));
                sendEvent(context, DATAHUB_DISK_UPDATE_HANDLER_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "DISK_RESIZE_STATE")
    public Action<?, ?> diskResizeInDatahubAction() {
        return new AbstractDistroXDiskUpdateAction<>(DistroXDiskUpdateEvent.class) {
            @Override
            protected void doExecute(CommonContext ctx, DistroXDiskUpdateEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting disk resize for stack: {}", payload.getResourceId());
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        UPDATE_IN_PROGRESS.name(),
                        DISK_RESIZE_STARTED,
                        String.valueOf(payload.getResourceId()));
                DistroXDiskUpdateEvent handlerRequest = DistroXDiskUpdateEvent.builder()
                        .withResourceCrn(payload.getResourceCrn())
                        .withResourceId(payload.getResourceId())
                        .withStackId(payload.getStackId())
                        .withDiskUpdateRequest(payload.getDiskUpdateRequest())
                        .withClusterName(payload.getClusterName())
                        .withAccountId(payload.getAccountId())
                        .withSelector(DATAHUB_DISK_RESIZE_HANDLER_EVENT.selector())
                        .build();
                sendEvent(ctx, DATAHUB_DISK_RESIZE_HANDLER_EVENT.event(), handlerRequest);
            }
        };
    }

    @Bean(name = "DATAHUB_DISK_UPDATE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractDistroXDiskUpdateAction<>(DistroXDiskResizeFinishedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DistroXDiskResizeFinishedEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Sending finalize event after disk resize for stack: {}", payload.getResourceId());
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                            Status.AVAILABLE.name(),
                            DATAHUB_DISK_UPDATE_FINISHED);
                sendEvent(context, DATAHUB_DISK_UPDATE_FINALIZE_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "DATAHUB_DISK_UPDATE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractDistroXDiskUpdateAction<>(DistroXDiskUpdateFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DistroXDiskUpdateFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Failed to update disks in Datahub {}. Status: {}.",
                        payload.getDiskUpdateEvent(), payload.getStatus(), payload.getException());
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        Status.UPDATE_FAILED.name(),
                        DATAHUB_DISK_UPDATE_FAILED,
                        payload.getException().getMessage());
                sendEvent(context, HANDLED_FAILED_DATAHUB_DISK_UPDATE_EVENT.selector(), payload);
            }
        };
    }
}
