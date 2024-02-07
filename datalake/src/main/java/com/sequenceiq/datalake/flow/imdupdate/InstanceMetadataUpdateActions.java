package com.sequenceiq.datalake.flow.imdupdate;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_IMD_UPDATE_FINISHED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_IMD_UPDATE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_IMD_UPDATE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;
import static com.sequenceiq.datalake.flow.imdupdate.SdxInstanceMetadataUpdateStateSelectors.SDX_IMD_UPDATE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.imdupdate.SdxInstanceMetadataUpdateStateSelectors.SDX_IMD_UPDATE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.metric.MetricType.IMD_UPDATE_FAILED;
import static com.sequenceiq.datalake.metric.MetricType.IMD_UPDATE_FINISHED;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.imdupdate.event.InstanceMetadataUpdateRequest;
import com.sequenceiq.datalake.flow.imdupdate.event.InstanceMetadataUpdateWaitRequest;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateEvent;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateFailedEvent;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateSuccessEvent;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateWaitSuccessEvent;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Configuration
public class InstanceMetadataUpdateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetadataUpdateActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxMetricService metricService;

    @Bean(name = "SDX_IMD_UPDATE_STATE")
    public Action<?, ?> callInstanceMetadataUpdateInCore() {
        return new AbstractInstanceMetadataUpdateSdxAction<>(SdxInstanceMetadataUpdateEvent.class) {

            @Override
            protected void doExecute(SdxContext context, SdxInstanceMetadataUpdateEvent payload, Map<Object, Object> variables) {
                Long sdxId = payload.getResourceId();
                LOGGER.info("Execute instance metadata update flow for SDX: {}", sdxId);
                sdxStatusService.setStatusForDatalakeAndNotify(DATALAKE_IMD_UPDATE_IN_PROGRESS, "Instance metadata update is in progress", sdxId);
                InstanceMetadataUpdateRequest request = InstanceMetadataUpdateRequest.from(context, payload.getUpdateType());
                sendEvent(context, request);
            }

            @Override
            protected Object getFailurePayload(SdxInstanceMetadataUpdateEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxInstanceMetadataUpdateFailedEvent.from(payload, ex, "Error launching instance metadata update");
            }
        };
    }

    @Bean(name = "SDX_IMD_UPDATE_WAIT_STATE")
    public Action<?, ?> waitForInstanceMetadataUpdateInCore() {
        return new AbstractInstanceMetadataUpdateSdxAction<>(SdxInstanceMetadataUpdateSuccessEvent.class) {

            @Override
            protected void doExecute(SdxContext context, SdxInstanceMetadataUpdateSuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Wait for instance metadata update flow in core for SDX: {}", payload.getResourceId());
                InstanceMetadataUpdateWaitRequest request = InstanceMetadataUpdateWaitRequest.from(context);
                sendEvent(context, request);
            }

        };
    }

    @Bean(name = "SDX_IMD_UPDATE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractInstanceMetadataUpdateSdxAction<>(SdxInstanceMetadataUpdateWaitSuccessEvent.class) {

            @Override
            protected void doExecute(SdxContext context, SdxInstanceMetadataUpdateWaitSuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Instance metadata update finalized for SDX: {}", payload.getResourceId());
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(RUNNING, DATALAKE_IMD_UPDATE_FINISHED,
                        "Datalake instance metadata update completed successfully", payload.getResourceId());
                metricService.incrementMetricCounter(IMD_UPDATE_FINISHED, sdxCluster);
                sendEvent(context, SDX_IMD_UPDATE_FINALIZED_EVENT.event(), payload);
            }

        };
    }

    @Bean(name = "SDX_IMD_UPDATE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractInstanceMetadataUpdateSdxAction<>(SdxInstanceMetadataUpdateFailedEvent.class) {

            @Override
            protected void doExecute(SdxContext context, SdxInstanceMetadataUpdateFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                String statusReason = exception.getMessage() != null ? exception.getMessage() : "Instance metadata update failed";
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DATALAKE_IMD_UPDATE_FAILED, Collections.singleton(statusReason),
                        statusReason, payload.getResourceId());
                metricService.incrementMetricCounter(IMD_UPDATE_FAILED, sdxCluster);
                LOGGER.error("Instance metadata update failed with:", exception);
                sendEvent(context, SDX_IMD_UPDATE_FAILED_HANDLED_EVENT.event(), payload);
            }

        };
    }
}
