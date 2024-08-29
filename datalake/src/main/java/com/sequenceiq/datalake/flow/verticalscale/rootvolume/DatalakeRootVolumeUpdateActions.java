package com.sequenceiq.datalake.flow.verticalscale.rootvolume;

import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.DATALAKE_ROOT_VOLUME_UPDATE_FINALIZE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.DATALAKE_ROOT_VOLUME_UPDATE_HANDLER_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.HANDLED_FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateEvent;
import com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateFailedEvent;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class DatalakeRootVolumeUpdateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRootVolumeUpdateActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Bean(name = "DATALAKE_ROOT_VOLUME_UPDATE_STATE")
    public Action<?, ?> rootVolumeUpdateAction() {
        return new AbstractDatalakeRootVolumeUpdateAction<>(DatalakeRootVolumeUpdateEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeRootVolumeUpdateEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting Root Volume Update for datalake.");
                sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(DatalakeStatusEnum.DATALAKE_DISK_UPDATE_IN_PROGRESS,
                        String.format("Root disk update is in progress for group of %s on the Data Lake.",
                                payload.getRootVolumeUpdateRequest().getGroup()), payload.getResourceId());
                sendEvent(context, DATALAKE_ROOT_VOLUME_UPDATE_HANDLER_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "DATALAKE_ROOT_VOLUME_UPDATE_FINISHED_STATE")
    public Action<?, ?> rootVolumeUpdateFinishedAction() {
        return new AbstractDatalakeRootVolumeUpdateAction<>(DatalakeRootVolumeUpdateEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeRootVolumeUpdateEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Root volume update has finished on the Data Lake.");
                sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(DatalakeStatusEnum.RUNNING,
                        String.format("Root volume update  of %s has finished on the Data Lake.",
                                payload.getRootVolumeUpdateRequest().getGroup()), payload.getResourceId());
                sendEvent(context, DATALAKE_ROOT_VOLUME_UPDATE_FINALIZE_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "DATALAKE_ROOT_VOLUME_UPDATE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractDatalakeRootVolumeUpdateAction<>(DatalakeRootVolumeUpdateFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeRootVolumeUpdateFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to update disks in DataLake '%s'. Status: '%s'.",
                        payload.getDatalakeRootVolumeUpdateEvent(), payload.getDatalakeStatus()), payload.getException());
                sdxStatusService.setStatusForDatalakeAndNotify(
                        payload.getDatalakeStatus(),
                        payload.getDatalakeStatus().getDefaultResourceEvent(),
                        payload.getException().getMessage(),
                        payload.getResourceId());
                sendEvent(context, HANDLED_FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT.selector(), payload);
            }
        };
    }
}
