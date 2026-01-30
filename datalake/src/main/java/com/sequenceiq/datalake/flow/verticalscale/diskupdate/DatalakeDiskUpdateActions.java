package com.sequenceiq.datalake.flow.verticalscale.diskupdate;

import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_FINALIZE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_HANDLER_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_VALIDATION_HANDLER_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.FAILED_DATALAKE_DISK_UPDATE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.HANDLED_FAILED_DATALAKE_DISK_UPDATE_EVENT;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateEvent;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateFailedEvent;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class DatalakeDiskUpdateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDiskUpdateActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Bean(name = "DATALAKE_DISK_UPDATE_VALIDATION_STATE")
    public Action<?, ?> diskUpdateValidationAction() {
        return new AbstractDatalakeDiskUpdateAction<>(DatalakeDiskUpdateEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeDiskUpdateEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting validations for datalake disk update.");
                sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(DatalakeStatusEnum.DATALAKE_DISK_UPDATE_VALIDATION_IN_PROGRESS,
                        String.format("Validation of disk update is in progress for group of %s on the Data Lake.",
                                payload.getDatalakeDiskUpdateRequest().getGroup()), payload.getResourceId());
                sendEvent(context, DATALAKE_DISK_UPDATE_VALIDATION_HANDLER_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "DATALAKE_DISK_UPDATE_STATE")
    public Action<?, ?> diskUpdateInDatalakeAction() {
        return new AbstractDatalakeDiskUpdateAction<>(DatalakeDiskUpdateEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeDiskUpdateEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting to update disks for datalake disk update.");
                sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(DatalakeStatusEnum.DATALAKE_DISK_UPDATE_IN_PROGRESS,
                    String.format("Disk Update is in progress for group of %s on the Data Lake.",
                        payload.getDatalakeDiskUpdateRequest().getGroup()), payload.getResourceId());
                sendEvent(context, DATALAKE_DISK_UPDATE_HANDLER_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "DATALAKE_DISK_UPDATE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractDatalakeDiskUpdateAction<>(DatalakeDiskUpdateEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeDiskUpdateEvent payload, Map<Object, Object> variables) {
                try {
                    sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(DatalakeStatusEnum.RUNNING,
                            "Disk update has finished on the Data Lake.", payload.getResourceId());
                    sendEvent(context, DATALAKE_DISK_UPDATE_FINALIZE_EVENT.selector(), payload);
                } catch (Exception ex) {
                    DatalakeDiskUpdateFailedEvent failedEvent = DatalakeDiskUpdateFailedEvent.builder()
                            .withDatalakeDiskUpdateEvent(payload)
                            .withException(ex)
                            .withDatalakeStatus(DatalakeStatusEnum.DATALAKE_DISK_UPDATE_FAILED).build();
                    LOGGER.error("Failed to update cloudbreak internal resources: {} on stack {}", ex.getMessage(), payload.getStackCrn());
                    sendEvent(context, FAILED_DATALAKE_DISK_UPDATE_EVENT.selector(), failedEvent);
                }
            }
        };
    }

    @Bean(name = "DATALAKE_DISK_UPDATE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractDatalakeDiskUpdateAction<>(DatalakeDiskUpdateFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeDiskUpdateFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to update disks in DataLake '%s'. Status: '%s'.",
                        payload.getDatalakeDiskUpdateEvent(), payload.getDatalakeStatus()), payload.getException());
                sdxStatusService.setStatusForDatalakeAndNotify(
                        payload.getDatalakeStatus(),
                        payload.getDatalakeStatus().getDefaultResourceEvent(),
                        List.of(payload.getException().getMessage()),
                        payload.getException().getMessage(),
                        payload.getResourceId());
                sendEvent(context, HANDLED_FAILED_DATALAKE_DISK_UPDATE_EVENT.event(), payload);
            }
        };
    }
}
