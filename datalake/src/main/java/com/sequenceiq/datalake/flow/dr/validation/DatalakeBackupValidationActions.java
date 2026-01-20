package com.sequenceiq.datalake.flow.dr.validation;

import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_BACKUP_VALIDATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_BACKUP_VALIDATION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_BACKUP_VALIDATION_IN_PROGRESS_EVENT;

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
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeBackupStatusResponse;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFailureReason;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeBackupValidationFailedEvent;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeBackupValidationWaitRequest;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeTriggerBackupValidationEvent;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Configuration
public class DatalakeBackupValidationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeBackupValidationActions.class);

    private static final String BACKUP_ID = "BACKUP-ID";

    private static final String REASON = "REASON";

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxMetricService metricService;

    @Inject
    private SdxService sdxService;

    @Bean(name = "DATALAKE_TRIGGERING_BACKUP_VALIDATION_STATE")
    public Action<?, ?> triggerDatalakeBackupValidationAction() {
        return new AbstractSdxAction<>(DatalakeTriggerBackupValidationEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeTriggerBackupValidationEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Triggering datalake backup validation for {}", payload.getResourceId());
                variables.put(REASON, payload.getReason().name());

                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_BACKUP_VALIDATION_IN_PROGRESS,
                        "Datalake backup validation in progress", payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.SDX_BACKUP_VALIDATION_REQUESTED, sdxCluster);

                DatalakeBackupStatusResponse backupStatusResponse =
                        sdxBackupRestoreService.triggerDatalakeBackupValidation(payload.getResourceId(), payload.getBackupLocation(), payload.getUserId());
                variables.put(BACKUP_ID, backupStatusResponse.getBackupId());
                payload.getDrStatus().setOperationId(backupStatusResponse.getBackupId());
                if (backupStatusResponse.getState().isFailed()) {
                    LOGGER.error("Failed to initiate backup validation for cluster {}.", sdxCluster.getClusterName());
                    throw new CloudbreakServiceException(backupStatusResponse.getFailureReason());
                } else {
                    sendEvent(context, DATALAKE_BACKUP_VALIDATION_IN_PROGRESS_EVENT.event(), payload);
                }
            }

            @Override
            protected Object getFailurePayload(DatalakeTriggerBackupValidationEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeBackupValidationFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_BACKUP_VALIDATION_IN_PROGRESS_STATE")
    public Action<?, ?> datalakeBackupValidationInProgressAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake backup validation in progress for {} ", payload.getResourceId());
                String backupId = (String) variables.get(BACKUP_ID);
                sendEvent(context, DatalakeBackupValidationWaitRequest.from(context, backupId));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeBackupValidationFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_BACKUP_VALIDATION_FINISHED_STATE")
    public Action<?, ?> finishedBackupValidationAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake backup validation finalized for cluster: {}", payload.getResourceId());
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_BACKUP_VALIDATION_FINISHED,
                        "Datalake backup validation finished", payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.SDX_BACKUP_VALIDATION_FINISHED, sdxCluster);
                sendEvent(context, DATALAKE_BACKUP_VALIDATION_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeBackupValidationFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_BACKUP_VALIDATION_FAILED_STATE")
    public Action<?, ?> backupValidationFailedAction() {
        return new AbstractSdxAction<>(DatalakeBackupValidationFailedEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeBackupValidationFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                String failureReason = getFailureReason(variables, exception);
                LOGGER.error(
                        "Datalake backup validation failed for datalake with id: {} with error: {}", payload.getResourceId(),
                        failureReason, exception
                );
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_BACKUP_VALIDATION_FAILED, Collections.singleton(failureReason),
                        failureReason, payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.SDX_BACKUP_VALIDATION_FAILED, sdxCluster);
                sendEvent(context, DATALAKE_BACKUP_VALIDATION_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeBackupValidationFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.error("Critical error! Failure was not handled correctly.", ex);
                return null;
            }

            private String getFailureReason(Map<Object, Object> variables, Exception exception) {
                StringBuilder reason = new StringBuilder();
                if (variables.containsKey(REASON) && variables.get(REASON).equals(DatalakeBackupFailureReason.BACKUP_ON_UPGRADE.name())) {
                    reason.append("Datalake backup validation failed during upgrade. Upgrade preparation not started. ");
                }
                if (exception instanceof PollerStoppedException) {
                    reason.append("Datalake backup validation timed out, see the backup status using cdp-cli for more information.");
                } else {
                    reason.append("Datalake backup validation failed.");
                }
                if (exception != null && StringUtils.isNotEmpty(exception.getMessage())) {
                    reason.append(" Failure message: ").append(exception.getMessage());
                }
                return reason.toString();
            }
        };
    }
}
