package com.sequenceiq.datalake.flow.dr.backup;

import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_BACKUP_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_BACKUP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_DATABASE_BACKUP_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_DATABASE_BACKUP_IN_PROGRESS_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeDrStatusResponse;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.operation.SdxOperationStatus;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeBackupFailedEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeBackupSuccessEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupCouldNotStartEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupFailedEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupStartEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupWaitRequest;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeFullBackupWaitRequest;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeTriggerBackupEvent;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.sdx.api.model.DatalakeDatabaseDrStatus;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupStatusResponse;

@Configuration
public class DatalakeBackupActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeBackupActions.class);

    private static final String OPERATION_ID = "OPERATION-ID";

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

    @Inject
    private EventSenderService eventSenderService;

    @Bean(name = "DATALAKE_TRIGGERING_BACKUP_STATE")
    public Action<?, ?> triggerDatalakeBackup() {
        return new AbstractSdxAction<>(DatalakeTriggerBackupEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeTriggerBackupEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void prepareExecution(DatalakeTriggerBackupEvent payload, Map<Object, Object> variables) {
                variables.put(OPERATION_ID, payload.getDrStatus().getOperationId());
                variables.put(REASON, payload.getReason().name());
                super.prepareExecution(payload, variables);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeTriggerBackupEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Triggering data lake backup for {}", payload.getResourceId());

                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                eventSenderService.sendEventAndNotification(sdxCluster, context.getFlowTriggerUserCrn(), ResourceEvent.DATALAKE_BACKUP_IN_PROGRESS);

                DatalakeDrStatusResponse backupStatusResponse =
                        sdxBackupRestoreService.triggerDatalakeBackup(payload.getResourceId(), payload.getBackupLocation(),
                                payload.getBackupName(), payload.getUserId());
                variables.put(BACKUP_ID, backupStatusResponse.getDrOperationId());
                variables.put(OPERATION_ID, backupStatusResponse.getDrOperationId());
                payload.getDrStatus().setOperationId(backupStatusResponse.getDrOperationId());
                if (!backupStatusResponse.failed()) {
                    sendEvent(context, DatalakeDatabaseBackupStartEvent.from(payload, backupStatusResponse.getDrOperationId()));
                } else {
                    sendEvent(context, DATALAKE_BACKUP_FAILED_EVENT.event(), payload);
                }
            }

            @Override
            protected Object getFailurePayload(DatalakeTriggerBackupEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeBackupFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_BACKUP_START_STATE")
    public Action<?, ?> datalakeBackup() {
        return new AbstractSdxAction<>(DatalakeDatabaseBackupStartEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeDatabaseBackupStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void prepareExecution(DatalakeDatabaseBackupStartEvent payload, Map<Object, Object> variables) {
                super.prepareExecution(payload, variables);
                if (!variables.containsKey(OPERATION_ID)) {
                    variables.put(OPERATION_ID, payload.getDrStatus().getOperationId());
                }
                if (!variables.containsKey(BACKUP_ID)) {
                    variables.put(BACKUP_ID, payload.getBackupRequest().getBackupId());
                }
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseBackupStartEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake database backup has been started for {}", payload.getResourceId());

                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                eventSenderService.sendEventAndNotification(sdxCluster, context.getFlowTriggerUserCrn(), ResourceEvent.DATALAKE_DATABASE_BACKUP);

                sdxBackupRestoreService.databaseBackup(payload.getDrStatus(),
                        payload.getResourceId(),
                        payload.getBackupRequest());
                sendEvent(context, DATALAKE_DATABASE_BACKUP_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseBackupStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseBackupCouldNotStartEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_BACKUP_IN_PROGRESS_STATE")
    public Action<?, ?> datalakeBackupInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake database backup is in progress for {} ", payload.getResourceId());
                String operationId = (String) variables.get(OPERATION_ID);
                sdxBackupRestoreService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.INPROGRESS, null);
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_BACKUP_INPROGRESS,
                        ResourceEvent.DATALAKE_BACKUP_IN_PROGRESS,
                        "Datalake backup in progress", payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.SDX_BACKUP_REQUESTED, sdxCluster);
                sendEvent(context, DatalakeDatabaseBackupWaitRequest.from(context, operationId));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseBackupFailedEvent.from(payload, ex);

            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_BACKUP_COULD_NOT_START_STATE")
    public Action<?, ?> backupCouldNotStart() {
        return new AbstractSdxAction<>(DatalakeDatabaseBackupCouldNotStartEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeDatabaseBackupCouldNotStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseBackupCouldNotStartEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Datalake database backup could not be started for datalake with id: {}", payload.getResourceId(), exception);
                String operationId = (String) variables.get(OPERATION_ID);
                sdxBackupRestoreService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.FAILED, payload.getException().getMessage());

                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                eventSenderService.sendEventAndNotification(sdxCluster, context.getFlowTriggerUserCrn(), ResourceEvent.DATALAKE_DATABASE_BACKUP_FAILED);

                sendEvent(context, DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseBackupCouldNotStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseBackupFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_FULL_BACKUP_IN_PROGRESS_STATE")
    public Action<?, ?> datalakeFullBackupInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Full datalake backup is in progress for {} ", payload.getResourceId());
                String operationId = (String) variables.get(OPERATION_ID);
                String backupId = (String) variables.get(BACKUP_ID);
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                SdxDatabaseBackupStatusResponse backupStatusResponse =
                        sdxBackupRestoreService.getDatabaseBackupStatus(sdxCluster, operationId);
                if (backupStatusResponse.getStatus().equals(DatalakeDatabaseDrStatus.INPROGRESS)) {
                    sdxBackupRestoreService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.SUCCEEDED, null);
                }
                sendEvent(context, DatalakeFullBackupWaitRequest.from(context, backupId));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeBackupFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_BACKUP_FINISHED_STATE")
    public Action<?, ?> finishedBackupAction() {
        return new AbstractSdxAction<>(DatalakeBackupSuccessEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeBackupSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeBackupSuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Sdx backup is finalized with sdx id: {}", payload.getResourceId());
                sendEvent(context, DATALAKE_DATABASE_BACKUP_FINALIZED_EVENT.event(), payload);
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_BACKUP_FINISHED,
                        "Datalake backup finished, Datalake is running", payload.getResourceId());

                eventSenderService.sendEventAndNotification(sdxCluster, context.getFlowTriggerUserCrn(), ResourceEvent.DATALAKE_BACKUP_FINISHED);

                metricService.incrementMetricCounter(MetricType.SDX_BACKUP_FINISHED, sdxCluster);
            }

            @Override
            protected Object getFailurePayload(DatalakeBackupSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseBackupFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_BACKUP_FAILED_STATE")
    public Action<?, ?> databaseBackupFailed() {
        return new AbstractSdxAction<>(DatalakeDatabaseBackupFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeDatabaseBackupFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseBackupFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Datalake database backup failed for datalake with id: {}", payload.getResourceId(), exception);
                String operationId = (String) variables.get(OPERATION_ID);
                sdxBackupRestoreService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.FAILED, exception.getLocalizedMessage());

                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                eventSenderService.sendEventAndNotification(sdxCluster, context.getFlowTriggerUserCrn(), ResourceEvent.DATALAKE_DATABASE_BACKUP_FAILED);

                sendEvent(context, DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseBackupFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseBackupFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_BACKUP_FAILED_STATE")
    public Action<?, ?> backupFailed() {
        return new AbstractSdxAction<>(DatalakeBackupFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeBackupFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeBackupFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Datalake backup failed for datalake with id: {}", payload.getResourceId(), exception);
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_BACKUP_FAILED,
                        getFailureReason(variables, exception), payload.getResourceId());

                metricService.incrementMetricCounter(MetricType.SDX_BACKUP_FAILED, sdxCluster);
                Flow flow = getFlow(context.getFlowParameters().getFlowId());
                flow.setFlowFailed(payload.getException());

                eventSenderService.sendEventAndNotification(sdxCluster, context.getFlowTriggerUserCrn(), ResourceEvent.DATALAKE_BACKUP_FAILED);

                sendEvent(context, DATALAKE_BACKUP_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeBackupFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseBackupFailedEvent.from(payload, ex);
            }
        };
    }

    private String getFailureReason(Map<Object, Object> variables, Exception exception) {
        StringBuilder reason = new StringBuilder();
        if (variables.containsKey(REASON) && variables.get(REASON).equals(DatalakeBackupFailureReason.BACKUP_ON_UPGRADE.name())) {
            reason.append("Upgrade not started, datalake backup failed.");
        } else {
            reason.append("Backup failed, returning datalake to running state.");
        }
        if (exception != null && StringUtils.isNotEmpty(exception.getMessage())) {
            reason.append(" Failure message: ").append(exception.getMessage());
        }
        return reason.toString();
    }
}
