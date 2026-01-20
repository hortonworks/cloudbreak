package com.sequenceiq.datalake.flow.dr.validation;

import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_RESTORE_VALIDATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_RESTORE_VALIDATION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_RESTORE_VALIDATION_IN_PROGRESS_EVENT;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeRestoreStatusResponse;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeRestoreValidationFailedEvent;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeRestoreValidationWaitRequest;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeTriggerRestoreValidationEvent;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Configuration
public class DatalakeRestoreValidationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRestoreValidationActions.class);

    private static final String RESTORE_ID = "RESTORE-ID";

    private static final String REASON = "REASON";

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxMetricService metricService;

    @Inject
    private SdxService sdxService;

    @Bean(name = "DATALAKE_TRIGGERING_RESTORE_VALIDATION_STATE")
    public Action<?, ?> triggerDatalakeRestoreValidationAction() {
        return new AbstractSdxAction<>(DatalakeTriggerRestoreValidationEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeTriggerRestoreValidationEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Triggering datalake restore validation for {}", payload.getResourceId());
                variables.put(REASON, payload.getReason().name());

                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_RESTORE_VALIDATION_IN_PROGRESS,
                        "Datalake restore validation in progress", payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.SDX_RESTORE_VALIDATION_REQUESTED, sdxCluster);

                DatalakeRestoreStatusResponse restoreStatusResponse =
                        sdxBackupRestoreService.triggerDatalakeRestoreValidation(sdxCluster, payload.getUserId());
                variables.put(RESTORE_ID, restoreStatusResponse.getRestoreId());
                payload.getDrStatus().setOperationId(restoreStatusResponse.getRestoreId());
                if (restoreStatusResponse.getState().isFailed()) {
                    LOGGER.error("Failed to initiate restore validation for cluster {}.", sdxCluster.getClusterName());
                    throw new CloudbreakServiceException(restoreStatusResponse.getFailureReason());
                } else {
                    sendEvent(context, DATALAKE_RESTORE_VALIDATION_IN_PROGRESS_EVENT.event(), payload);
                }
            }

            @Override
            protected Object getFailurePayload(DatalakeTriggerRestoreValidationEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeRestoreValidationFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_RESTORE_VALIDATION_IN_PROGRESS_STATE")
    public Action<?, ?> datalakeRestoreValidationInProgressAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake restore validation in progress for {} ", payload.getResourceId());
                String restoreId = (String) variables.get(RESTORE_ID);
                sendEvent(context, DatalakeRestoreValidationWaitRequest.from(context, restoreId));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeRestoreValidationFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_RESTORE_VALIDATION_FINISHED_STATE")
    public Action<?, ?> finishedRestoreValidationAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake restore validation finalized for cluster: {}", payload.getResourceId());
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_RESTORE_VALIDATION_FINISHED,
                        "Datalake restore validation finished", payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.SDX_RESTORE_VALIDATION_FINISHED, sdxCluster);
                sendEvent(context, DATALAKE_RESTORE_VALIDATION_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeRestoreValidationFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_RESTORE_VALIDATION_FAILED_STATE")
    public Action<?, ?> restoreValidationFailedAction() {
        return new AbstractSdxAction<>(DatalakeRestoreValidationFailedEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeRestoreValidationFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                String failureReason = getFailureReason(variables, exception);
                LOGGER.error(
                        "Datalake restore validation failed for datalake with id: {} with error: {}", payload.getResourceId(),
                        failureReason, exception
                );
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_RESTORE_VALIDATION_FAILED, Collections.singleton(failureReason),
                        failureReason, payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.SDX_RESTORE_VALIDATION_FAILED, sdxCluster);

                sendEvent(context, DATALAKE_RESTORE_VALIDATION_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeRestoreValidationFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.error("Critical error! Failure was not handled correctly.", ex);
                return null;
            }

            private String getFailureReason(Map<Object, Object> variables, Exception exception) {
                StringBuilder reason = new StringBuilder();
                if (exception instanceof PollerStoppedException) {
                    reason.append("Datalake restore validation timed out, see the restore status using cdp-cli for more information.");
                } else {
                    reason.append("Datalake restore validation failed.");
                }
                if (exception != null && StringUtils.isNotEmpty(exception.getMessage())) {
                    reason.append(" Failure message: ").append(exception.getMessage());
                }
                return reason.toString();
            }
        };
    }
}
