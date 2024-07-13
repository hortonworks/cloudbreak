package com.sequenceiq.datalake.flow.certrotation;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_DATABASE_CERTIFICATE_ROTATION_FAILED;
import static com.sequenceiq.datalake.flow.certrotation.RotateCertificateStateSelectors.ROTATE_CERTIFICATE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.certrotation.RotateCertificateStateSelectors.ROTATE_CERTIFICATE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.metric.MetricType.ROTATE_DATABASE_CERTIFICATE_FAILED;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.certrotation.event.RotateCertificateFailedEvent;
import com.sequenceiq.datalake.flow.certrotation.event.RotateCertificateStackEvent;
import com.sequenceiq.datalake.flow.certrotation.event.RotateCertificateStackRequest;
import com.sequenceiq.datalake.flow.certrotation.event.RotateCertificateSuccessEvent;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Configuration
public class RotateCertificateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateCertificateActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxMetricService metricService;

    @Bean(name = "ROTATE_CERTIFICATE_STACK_STATE")
    public Action<?, ?> rotateCertificateStack() {
        return new AbstractRotateCertificateSdxAction<>(RotateCertificateStackEvent.class) {

            @Override
            protected void doExecute(SdxContext context, RotateCertificateStackEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Execute Rotate Certificate stack flow for SDX: {}", payload.getResourceId());
                RotateCertificateStackRequest request = RotateCertificateStackRequest.from(context);
                sendEvent(context, request);
            }

            @Override
            protected Object getFailurePayload(RotateCertificateStackEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return RotateCertificateFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "ROTATE_CERTIFICATE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractRotateCertificateSdxAction<>(RotateCertificateSuccessEvent.class) {

            @Override
            protected void doExecute(SdxContext context, RotateCertificateSuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Rotate Certificate finalized for SDX: {}", payload.getResourceId());
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        "Rotate Certificate completed successfully", payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.ROTATE_DATABASE_CERTIFICATE_FINISHED, sdxCluster);
                sendEvent(context, ROTATE_CERTIFICATE_FINALIZED_EVENT.event(), payload);
            }

        };
    }

    @Bean(name = "ROTATE_CERTIFICATE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractRotateCertificateSdxAction<>(RotateCertificateFailedEvent.class) {

            @Override
            protected void doExecute(SdxContext context, RotateCertificateFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                DatalakeStatusEnum failedStatus = DATALAKE_DATABASE_CERTIFICATE_ROTATION_FAILED;
                LOGGER.info("Update SDX status to {} for resource: {}", failedStatus, payload.getResourceId(), exception);
                String statusReason = "Rotate Certificate failed";
                if (exception.getMessage() != null) {
                    statusReason = exception.getMessage();
                }
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(failedStatus, statusReason, payload.getResourceId());
                metricService.incrementMetricCounter(ROTATE_DATABASE_CERTIFICATE_FAILED, sdxCluster);
                sendEvent(context, ROTATE_CERTIFICATE_FAILED_HANDLED_EVENT.event(), payload);
            }

        };
    }
}
