package com.sequenceiq.datalake.flow.dr.restore;

import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreEvent.DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreEvent.DATALAKE_DATABASE_RESTORE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreEvent.DATALAKE_DATABASE_RESTORE_IN_PROGRESS_EVENT;

import com.sequenceiq.datalake.entity.operation.SdxOperationStatus;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreCouldNotStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreFailedEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreFailedEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreSuccessEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreWaitRequest;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeFullRestoreWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.dr.SdxDatabaseDrService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

@Configuration
public class DatalakeDatabaseRestoreActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDatabaseRestoreActions.class);

    private static final String OPERATION_ID = "OPERATION-ID";

    private static final String BACKUP_ID = "BACKUP-ID";

    private static final String RESTORE_ID = "RESTORE-ID";

    @Inject
    private SdxDatabaseDrService sdxDatabaseDrService;

    @Bean(name = "DATALAKE_DATABASE_RESTORE_START_STATE")
    public Action<?, ?> datalakeRestore() {
        return new AbstractSdxAction<>(DatalakeDatabaseRestoreStartEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeDatabaseRestoreStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void prepareExecution(DatalakeDatabaseRestoreStartEvent payload, Map<Object, Object> variables) {
                super.prepareExecution(payload, variables);
                variables.put(BACKUP_ID, payload.getBackupId());
                variables.put(RESTORE_ID, payload.getRestoreId());
                variables.put(OPERATION_ID, payload.getDrStatus().getOperationId());
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseRestoreStartEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake database restore has been started for {}", payload.getResourceId());
                sdxDatabaseDrService.databaseRestore(payload.getDrStatus(),
                        payload.getResourceId(),
                        payload.getBackupId(),
                        payload.getBackupLocation());
                sendEvent(context, DATALAKE_DATABASE_RESTORE_IN_PROGRESS_EVENT.event(), payload);
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
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake database restore is in progress for {} ", payload.getResourceId());
                String operationId = (String) variables.get(OPERATION_ID);
                sdxDatabaseDrService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.INPROGRESS, null);
                sendEvent(context, DatalakeDatabaseRestoreWaitRequest.from(context, operationId));
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
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeDatabaseRestoreCouldNotStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseRestoreCouldNotStartEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Datalake database restore could not be started for datalake with id: {}", payload.getResourceId(), exception);
                String operationId = (String) variables.get(OPERATION_ID);
                sdxDatabaseDrService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.FAILED, exception.getLocalizedMessage());
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
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Full datalake restore is in progress for {} ", payload.getResourceId());
                String operationId = (String) variables.get(OPERATION_ID);
                String restoreId = (String) variables.get(RESTORE_ID);
                sdxDatabaseDrService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.SUCCEEDED, null);
                sendEvent(context, DatalakeFullRestoreWaitRequest.from(context, restoreId));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseRestoreFailedEvent.from(payload, ex);

            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_RESTORE_FINISHED_STATE")
    public Action<?, ?> finishedRestoreAction() {
        return new AbstractSdxAction<>(DatalakeRestoreSuccessEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeRestoreSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeRestoreSuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Sdx database restore is finalized with sdx id: {}", payload.getResourceId());
                sendEvent(context, DATALAKE_DATABASE_RESTORE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeRestoreSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseRestoreFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_RESTORE_FAILED_STATE")
    public Action<?, ?> databaseRestoreFailed() {
        return new AbstractSdxAction<>(DatalakeDatabaseRestoreFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeDatabaseRestoreFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseRestoreFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Datalake database restore could not be started for datalake with id: {}", payload.getResourceId(), exception);
                String operationId = (String) variables.get(OPERATION_ID);
                sdxDatabaseDrService.updateDatabaseStatusEntry(operationId, SdxOperationStatus.FAILED, exception.getLocalizedMessage());
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
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeRestoreFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeRestoreFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Datalake database restore could not be started for datalake with id: {}", payload.getResourceId(), exception);
                sendEvent(context, DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeRestoreFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeRestoreFailedEvent.from(payload, ex);
            }
        };
    }
}
