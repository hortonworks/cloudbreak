package com.sequenceiq.datalake.flow.dr.backup;

import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupEvent.DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupEvent.DATALAKE_DATABASE_BACKUP_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupEvent.DATALAKE_DATABASE_BACKUP_IN_PROGRESS_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.entity.operation.SdxOperationStatus;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupCouldNotStartEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupFailedEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupStartEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupSuccessEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.dr.SdxDatabaseDrService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class DatalakeDatabaseBackupActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDatabaseBackupActions.class);

    private static final String OPERATION_ID = "OPERATION-ID";

    @Inject
    private SdxDatabaseDrService sdxDatabaseDrService;

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
                variables.put(OPERATION_ID, payload.getDrStatus().getOperationId());
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseBackupStartEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake database backup has been started for {}", payload.getResourceId());
                sdxDatabaseDrService.databaseBackup(payload.getDrStatus(),
                        payload.getResourceId(),
                        payload.getBackupId(),
                        payload.getBackupLocation());
                sendEvent(context, DATALAKE_DATABASE_BACKUP_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseBackupStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseBackupCouldNotStartEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_BACKUP_IN_PROGRESS_STATE")
    public Action<?, ?> datalakebackupInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake database backup is in progress for {} ", payload.getResourceId());
                String operationId = (String) variables.get(OPERATION_ID);
                sdxDatabaseDrService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.INPROGRESS, null);
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
                sdxDatabaseDrService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.FAILED, payload.getException().getMessage());
                sendEvent(context, DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseBackupCouldNotStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseBackupFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_BACKUP_FINISHED_STATE")
    public Action<?, ?> finishedBackupAction() {
        return new AbstractSdxAction<>(DatalakeDatabaseBackupSuccessEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeDatabaseBackupSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseBackupSuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Sdx database backup is finalized with sdx id: {}", payload.getResourceId());
                String operationId = (String) variables.get(OPERATION_ID);
                sdxDatabaseDrService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.SUCCEEDED, null);
                sendEvent(context, DATALAKE_DATABASE_BACKUP_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseBackupSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseBackupFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_BACKUP_FAILED_STATE")
    public Action<?, ?> backupFailed() {
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
                sdxDatabaseDrService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.FAILED, exception.getLocalizedMessage());
                sendEvent(context, DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseBackupFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseBackupFailedEvent.from(payload, ex);
            }
        };
    }
}
