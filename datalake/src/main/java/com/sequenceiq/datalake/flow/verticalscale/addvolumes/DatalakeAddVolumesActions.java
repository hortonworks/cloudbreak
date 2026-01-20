package com.sequenceiq.datalake.flow.verticalscale.addvolumes;

import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.DATALAKE_ADD_VOLUMES_FINALIZE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.DATALAKE_ADD_VOLUMES_HANDLER_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.HANDLED_FAILED_DATALAKE_ADD_VOLUMES_EVENT;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesEvent;
import com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesFailedEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Configuration
public class DatalakeAddVolumesActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeAddVolumesActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Bean(name = "DATALAKE_ADD_VOLUMES_STATE")
    public Action<?, ?> addVolumesAction() {
        return new AbstractSdxAction<>(DatalakeAddVolumesEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeAddVolumesEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting Datalake Add Volumes flow.");
                sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(DatalakeStatusEnum.DATALAKE_ADD_VOLUMES_IN_PROGRESS,
                        String.format("Calling core services to trigger add volumes flow for group of %s on the Data Lake.",
                                payload.getStackAddVolumesRequest().getInstanceGroup()), payload.getResourceId());
                DatalakeAddVolumesEvent datalakeAddVolumesEvent = new DatalakeAddVolumesEvent(DATALAKE_ADD_VOLUMES_HANDLER_EVENT.selector(),
                        payload.getResourceId(), payload.getUserId(), payload.getStackAddVolumesRequest(), payload.getSdxName());
                sendEvent(context, DATALAKE_ADD_VOLUMES_HANDLER_EVENT.selector(), datalakeAddVolumesEvent);
            }

            @Override
            protected Object getFailurePayload(DatalakeAddVolumesEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new DatalakeAddVolumesFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "DATALAKE_ADD_VOLUMES_FINISHED_STATE")
    public Action<?, ?> addVolumesFinishedAction() {
        return new AbstractSdxAction<>(DatalakeAddVolumesEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeAddVolumesEvent payload, Map<Object, Object> variables) {
                sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(DatalakeStatusEnum.RUNNING,
                        "Adding volumes has finished on the Data Lake.", payload.getResourceId());
                DatalakeAddVolumesEvent datalakeDiskFinalizeEvent = new DatalakeAddVolumesEvent(DATALAKE_ADD_VOLUMES_FINALIZE_EVENT.selector(),
                        payload.getResourceId(), payload.getUserId(), payload.getStackAddVolumesRequest(), payload.getSdxName());
                sendEvent(context, DATALAKE_ADD_VOLUMES_FINALIZE_EVENT.selector(), datalakeDiskFinalizeEvent);
            }

            @Override
            protected Object getFailurePayload(DatalakeAddVolumesEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new DatalakeAddVolumesFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "DATALAKE_ADD_VOLUMES_FAILED_STATE")
    public Action<?, ?> addVolumesFailedAction() {
        return new AbstractSdxAction<>(DatalakeAddVolumesFailedEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeAddVolumesFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.warn("Failed to add volumes in DataLake.", exception);
                String statusReason = "Datalake add volumes on stack failed";
                if (exception.getMessage() != null) {
                    statusReason = exception.getMessage();
                }
                sdxStatusService.setStatusForDatalakeAndNotify(
                        DatalakeStatusEnum.RUNNING,
                        DatalakeStatusEnum.DATALAKE_ADD_VOLUMES_FAILED.getDefaultResourceEvent(),
                        statusReason,
                        payload.getResourceId());
                sendEvent(context, HANDLED_FAILED_DATALAKE_ADD_VOLUMES_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeAddVolumesFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new DatalakeAddVolumesFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }
}
