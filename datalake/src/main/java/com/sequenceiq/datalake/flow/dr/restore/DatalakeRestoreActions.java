package com.sequenceiq.datalake.flow.dr.restore;

import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_DATABASE_RESTORE_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_RESTORE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_RESTORE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_RESTORE_FINALIZED_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.dyngr.exception.PollerStoppedException;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeRestoreStatusResponse;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.operation.SdxOperationStatus;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.chain.DatalakeResizeFlowEventChainFactory;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreCouldNotStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreFailedEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreWaitRequest;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeFullRestoreWaitRequest;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreAwaitServicesStoppedRequest;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreFailedEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreFailureHandledEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreSuccessEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeTriggerRestoreEvent;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.sdx.api.model.DatalakeDatabaseDrStatus;
import com.sequenceiq.sdx.api.model.SdxBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreStatusResponse;

@Configuration
public class DatalakeRestoreActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRestoreActions.class);

    private static final String OPERATION_ID = "OPERATION-ID";

    private static final String BACKUP_ID = "BACKUP-ID";

    private static final String RESTORE_ID = "RESTORE-ID";

    private static final String REASON = "REASON";

    private static final String MAX_DURATION_IN_MIN = "MAX_DURATION_IN_MIN";

    private static final String VALIDATION_ONLY = "VALIDATION_ONLY";

    private static final String FAILURE_REASON = "FAILURE_REASON";

    private static final double RESTORE_TIMOUET_MULTIPLIER = 1.5;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxMetricService metricService;

    @Inject
    private SdxService sdxService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Inject
    private EventSenderService eventSenderService;

    @Bean(name = "DATALAKE_TRIGGERING_RESTORE_STATE")
    public Action<?, ?> triggerDatalakeRestore() {
        return new AbstractSdxAction<>(DatalakeTriggerRestoreEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeTriggerRestoreEvent payload) {
                SdxContext sdxContext = SdxContext.from(flowParameters, payload);

                // When SDX is created as part of re-size flow chain, SDX in payload will not have the correct ID.
                if (flowChainLogService.isFlowTriggeredByFlowChain(
                        DatalakeResizeFlowEventChainFactory.class.getSimpleName(),
                        flowLogService.getLastFlowLog(flowParameters.getFlowId())) && !payload.isValidationOnly()) {
                    SdxCluster sdxCluster = sdxService.getByNameInAccount(payload.getUserId(), payload.getSdxName());
                    LOGGER.info("Updating the Sdx-id in context from {} to {}", payload.getResourceId(), sdxCluster.getId());
                    payload.getDrStatus().setSdxClusterId(sdxCluster.getId());
                    sdxContext.setSdxId(sdxCluster.getId());
                }
                return sdxContext;
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeTriggerRestoreEvent payload, Map<Object, Object> variables) {
                DatalakeRestoreStatusResponse restoreStatusResponse =
                        sdxBackupRestoreService.triggerDatalakeRestore(context.getSdxId(),
                                payload.getBackupId(),
                                payload.getBackupLocationOverride(),
                                payload.getUserId(),
                                payload.getSkipOptions(),
                                payload.isValidationOnly());
                variables.put(REASON, payload.getReason().name());
                variables.put(RESTORE_ID, restoreStatusResponse.getRestoreId());
                variables.put(BACKUP_ID, restoreStatusResponse.getBackupId());
                variables.put(OPERATION_ID, restoreStatusResponse.getRestoreId());

                // Update full restore polling duration
                SdxBackupStatusResponse sdxBackupStatusResponse = sdxBackupRestoreService
                        .getDatalakeBackupStatus(payload.getSdxName(), restoreStatusResponse.getBackupId(),
                                null, payload.getUserId());
                int restoreFullMaxDuration = (int) (sdxBackupRestoreService.getTotalDurationInMin(sdxBackupStatusResponse.getTimestamp(),
                                        sdxBackupStatusResponse.getEndTimestamp()) * RESTORE_TIMOUET_MULTIPLIER);
                variables.put(MAX_DURATION_IN_MIN, restoreFullMaxDuration);
                variables.put(VALIDATION_ONLY, payload.isValidationOnly());
                payload.getDrStatus().setOperationId(restoreStatusResponse.getRestoreId());
                if (restoreStatusResponse.getState().isFailed()) {
                    variables.put(FAILURE_REASON, restoreStatusResponse.getFailureReason());
                    LOGGER.error("Datalake restore has failed for {} ", context.getSdxId());
                    sendEvent(context, DATALAKE_RESTORE_FAILED_EVENT.event(), payload);
                } else {
                    sendEvent(context, DatalakeDatabaseRestoreStartEvent.from(payload, context.getSdxId(), restoreStatusResponse.getBackupId(),
                            restoreStatusResponse.getRestoreId()));
                }
            }

            @Override
            protected Object getFailurePayload(DatalakeTriggerRestoreEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeRestoreFailedEvent.from(flowContext, payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_RESTORE_AWAIT_SERVICES_STOPPED_STATE")
    public Action<?, ?> datalakeRestoreAwaitingServicesStopped() {
        return new AbstractSdxAction<>(DatalakeDatabaseRestoreStartEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseRestoreStartEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Wating for services to be stopped for datalake restore of {} ", payload.getResourceId());
                sendEvent(context, DatalakeRestoreAwaitServicesStoppedRequest.from(payload));
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseRestoreStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeRestoreFailedEvent.from(flowContext, payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_RESTORE_START_STATE")
    public Action<?, ?> datalakeRestore() {
        return new AbstractSdxAction<>(DatalakeDatabaseRestoreStartEvent.class) {
            @Override
            protected void prepareExecution(DatalakeDatabaseRestoreStartEvent payload, Map<Object, Object> variables) {
                super.prepareExecution(payload, variables);
                if (!variables.containsKey(BACKUP_ID)) {
                    variables.put(BACKUP_ID, payload.getBackupId());
                }
                if (!Strings.isNullOrEmpty(payload.getRestoreId())) {
                    variables.put(RESTORE_ID, payload.getRestoreId());
                }
                if (!variables.containsKey(OPERATION_ID)) {
                    variables.put(OPERATION_ID, payload.getDrStatus().getOperationId());
                }
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseRestoreStartEvent payload, Map<Object, Object> variables) {
                if (payload.getBackupLocation() != null) {
                    LOGGER.info("Datalake database restore has been started for {} with backup location {}", payload.getResourceId(),
                            payload.getBackupLocation());

                    int fullDrMaxDuration = variables.containsKey(MAX_DURATION_IN_MIN) ?
                            (Integer) variables.get(MAX_DURATION_IN_MIN) : 0;
                    int payloadDuration = payload.getDatabaseMaxDurationInMin();
                    int finalDuration = Math.max(fullDrMaxDuration, payloadDuration);
                    LOGGER.info("Using duration {} minutes (comparing payload duration {} and full DR duration {})",
                            finalDuration, payloadDuration, fullDrMaxDuration);
                    sdxBackupRestoreService.databaseRestore(payload.getDrStatus(),
                            payload.getResourceId(),
                            payload.getBackupId(),
                            payload.getBackupLocation(),
                            finalDuration,
                            (Boolean) variables.getOrDefault(VALIDATION_ONLY, false));

                    variables.put(MAX_DURATION_IN_MIN, finalDuration);
                    sendEvent(context, DATALAKE_DATABASE_RESTORE_IN_PROGRESS_EVENT.event(), payload);
                } else {
                    LOGGER.error("Datalake database restore has been skipped because backup location is null {}", payload.getResourceId());
                    throw new BadRequestException("Backup Location is null. Datalake database restore is not triggered.");
                }
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseRestoreStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseRestoreCouldNotStartEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_RESTORE_IN_PROGRESS_STATE")
    public Action<?, ?> datalakeRestoreInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake database restore is in progress for {} ", payload.getResourceId());
                String operationId = (String) variables.get(OPERATION_ID);
                int databaseMaxDurationInMin = variables.get(MAX_DURATION_IN_MIN) != null ? (Integer) variables.get(MAX_DURATION_IN_MIN) : 0;
                sdxBackupRestoreService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.INPROGRESS, null);
                if ((Boolean) variables.getOrDefault(VALIDATION_ONLY, Boolean.FALSE)) {
                    sdxBackupRestoreService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.SUCCEEDED, null);
                    SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_RESTORE_VALIDATION_INPROGRESS,
                        ResourceEvent.DATALAKE_RESTORE_VALIDATION_IN_PROGRESS,
                        "Datalake restore validation in progress", payload.getResourceId());
                } else {
                    SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_RESTORE_INPROGRESS,
                        ResourceEvent.DATALAKE_RESTORE_IN_PROGRESS,
                        "Datalake restore in progress", payload.getResourceId());
                    metricService.incrementMetricCounter(MetricType.SDX_RESTORE_REQUESTED, sdxCluster);
                }
                sendEvent(context, DatalakeDatabaseRestoreWaitRequest.from(context, operationId, databaseMaxDurationInMin));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseRestoreFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_RESTORE_COULD_NOT_START_STATE")
    public Action<?, ?> restoreCouldNotStart() {
        return new AbstractSdxAction<>(DatalakeDatabaseRestoreCouldNotStartEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseRestoreCouldNotStartEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Datalake database restore could not be started for datalake with id: {}", payload.getResourceId(), exception);

                // Due to the unique approach of this flow configuration, a failed database backup state is not considered a failed flow.
                getFlow(context.getFlowId()).clearFlowFailed();

                String operationId = (String) variables.get(OPERATION_ID);
                sdxBackupRestoreService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.FAILED, exception.getLocalizedMessage());
                sendEvent(context, DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseRestoreCouldNotStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseRestoreFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_FULL_RESTORE_IN_PROGRESS_STATE")
    public Action<?, ?> datalakeFullResoreInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Full datalake restore is in progress for {} ", payload.getResourceId());

                // Due to the unique approach of this flow configuration, a failed database backup state is not considered a failed flow.
                getFlow(context.getFlowId()).clearFlowFailed();

                String operationId = (String) variables.get(OPERATION_ID);
                String restoreId = (String) variables.get(RESTORE_ID);
                int fullDrMaxDurationInMin = variables.get(MAX_DURATION_IN_MIN) != null ? (Integer) variables.get(MAX_DURATION_IN_MIN) : 0;
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                if (variables.getOrDefault(VALIDATION_ONLY, false).equals(Boolean.FALSE)) {
                    SdxDatabaseRestoreStatusResponse restoreStatusResponse =
                        sdxBackupRestoreService.getDatabaseRestoreStatus(sdxCluster, operationId);
                    if (restoreStatusResponse.getStatus().equals(DatalakeDatabaseDrStatus.INPROGRESS)) {
                        sdxBackupRestoreService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.SUCCEEDED, null);
                    }
                }
                sendEvent(context, DatalakeFullRestoreWaitRequest.from(context, restoreId, fullDrMaxDurationInMin));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeRestoreFailedEvent.from(flowContext, payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_RESTORE_FINISHED_STATE")
    public Action<?, ?> finishedRestoreAction() {
        return new AbstractSdxAction<>(DatalakeRestoreSuccessEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeRestoreSuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Sdx database restore is finalized with sdx id: {}", payload.getResourceId());
                if (variables.containsKey(VALIDATION_ONLY) && (Boolean) variables.get(VALIDATION_ONLY)) {
                    SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_RESTORE_VALIDATION_FINISHED,
                        "Datalake restore validation finished, Datalake is running", payload.getResourceId());
                } else {
                    SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_RESTORE_FINISHED,
                        "Datalake restore finished, Datalake is running", payload.getResourceId());
                    metricService.incrementMetricCounter(MetricType.SDX_RESTORE_FINISHED, sdxCluster);
                }
                sendEvent(context, DATALAKE_RESTORE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeRestoreSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeRestoreFailedEvent.from(flowContext, payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_RESTORE_FAILED_STATE")
    public Action<?, ?> databaseRestoreFailed() {
        return new AbstractSdxAction<>(DatalakeDatabaseRestoreFailedEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseRestoreFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Datalake database restore could not be started for datalake with id: {}", payload.getResourceId(), exception);
                String operationId = (String) variables.get(OPERATION_ID);
                sdxBackupRestoreService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.FAILED, exception.getLocalizedMessage());
                sendEvent(context, DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseRestoreFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseRestoreFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_RESTORE_FAILED_STATE")
    public Action<?, ?> restoreFailed() {
        return new AbstractSdxAction<>(DatalakeRestoreFailedEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeRestoreFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Datalake restore failed for datalake with id: {}", payload.getResourceId(), exception);
                if ((Boolean) variables.getOrDefault(VALIDATION_ONLY, Boolean.FALSE)) {
                    String failureReason = getFailureReason(variables, exception);
                    SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_RESTORE_VALIDATION_FAILED,
                        List.of(failureReason), failureReason, payload.getResourceId());
                } else {
                    SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_RESTORE_FAILED,
                        getFailureReason(variables, exception), payload.getResourceId());
                    metricService.incrementMetricCounter(MetricType.SDX_RESTORE_FAILED, sdxCluster);
                }

                Optional<FlowLogWithoutPayload> lastFlowLog = flowLogService.getLastFlowLog(context.getFlowParameters().getFlowId());
                if (flowChainLogService.isFlowTriggeredByFlowChain(
                        DatalakeResizeFlowEventChainFactory.class.getSimpleName(),
                        lastFlowLog) && variables.containsKey(VALIDATION_ONLY) && !(Boolean) variables.get(VALIDATION_ONLY)) {
                    eventSenderService.notifyEvent(context, ResourceEvent.DATALAKE_RESIZE_FAILED_DURING_RESTORE);
                }

                sendEvent(context, DATALAKE_RESTORE_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeRestoreFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.error("Failed to perform normal restore failure actions. This will be ignored to properly end the flow.", ex);
                return DatalakeRestoreFailureHandledEvent.from(flowContext, payload);
            }

            private String getFailureReason(Map<Object, Object> variables, Exception exception) {
                StringBuilder reason = new StringBuilder();
                if ((Boolean) variables.getOrDefault(VALIDATION_ONLY, Boolean.FALSE) && variables.containsKey(FAILURE_REASON)) {
                    reason.append("Restore validation failed. Reason: ").append(variables.get(FAILURE_REASON));
                } else if (variables.containsKey(REASON) && variables.get(REASON).equals(DatalakeRestoreFailureReason.RESTORE_ON_UPGRADE_FAILURE.name())) {
                    reason.append("Upgrade not finished correctly, datalake restore failed.");
                } else {
                    if (exception instanceof PollerStoppedException) {
                        reason.append("Restore timed out, see the restore status using cdp-cli for more information.");
                    } else {
                        reason.append("Restore failed, returning datalake to running state.");
                    }
                }
                if (exception != null && StringUtils.isNotEmpty(exception.getMessage())) {
                    reason.append(" Failure message: ").append(exception.getMessage());
                }
                return reason.toString();
            }
        };
    }
}
