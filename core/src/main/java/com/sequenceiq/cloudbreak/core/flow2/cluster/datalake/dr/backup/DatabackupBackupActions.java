package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.BackupRestoreStatusService;
import com.sequenceiq.cloudbreak.core.flow2.event.DatabaseBackupTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.DatabaseBackupFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.DatabaseBackupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.DatabaseBackupSuccess;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.Flow;
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

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.DATABASE_BACKUP_FAILED_EVENT;

@Configuration
public class DatabackupBackupActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabackupBackupActions.class);

    @Inject
    private BackupRestoreStatusService backupRestoreStatusService;

    @Inject
    private StackService stackService;

    @Bean(name = "DATABASE_BACKUP_STATE")
    public Action<?, ?> backupDatabase() {
        LOGGER.info("HER DatabaseBackupActions.backupDatabase");
        return new AbstractDatabaseBackupAction<>(DatabaseBackupTriggerEvent.class) {

            @Override
            protected DatabaseBackupContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatabaseBackupTriggerEvent payload) {
                LOGGER.info("HER AbstractDatabaseBackupAction.createFlowContext");
                return DatabaseBackupContext.from(flowParameters, payload, payload.getBackupLocation());
            }

            @Override
            protected void doExecute(DatabaseBackupContext context, DatabaseBackupTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.info("HER AbstractDatabaseBackupAction.doExecute");
                backupRestoreStatusService.backupDatabase(context.getStackId(), payload.getBackupLocation());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(DatabaseBackupContext context) {
                LOGGER.info("HER AbstractDatabaseBackupAction.createRequest");
                return new DatabaseBackupRequest(context.getStackId(), context.getBackupLocation());
            }

            @Override
            protected Object getFailurePayload(DatabaseBackupTriggerEvent payload, Optional<DatabaseBackupContext> flowContext, Exception ex) {
                LOGGER.info("HER AbstractDatabaseBackupAction.getFailurePayload");
                return DatabaseBackupFailedEvent.from(payload, ex, DetailedStackStatus.DATABASE_BACKUP_FAILED);
            }
        };
    }

    @Bean(name = "DATABASE_BACKUP_FINISHED_STATE")
    public Action<?, ?> databaseBackupFinished() {
        return new AbstractDatabaseBackupAction<>(DatabaseBackupSuccess.class) {

            @Override
            protected DatabaseBackupContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatabaseBackupSuccess payload) {
                return DatabaseBackupContext.from(flowParameters, payload, null);
            }

            @Override
            protected void doExecute(DatabaseBackupContext context, DatabaseBackupSuccess payload, Map<Object, Object> variables) {
                backupRestoreStatusService.backupDatabaseFinished(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(DatabaseBackupContext context) {
                return new StackEvent(DatabaseBackupEvent.DATABASE_BACKUP_FINALIZED_EVENT.event(), context.getStackId());
            }

            @Override
            protected Object getFailurePayload(DatabaseBackupSuccess payload, Optional<DatabaseBackupContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "DATABASE_BACKUPS_FAILED_STATE")
    public Action<?, ?> databaseBackupFailedAction() {
        return new AbstractDatabaseBackupAction<>(DatabaseBackupFailedEvent.class) {

            @Override
            protected DatabaseBackupContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatabaseBackupFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                Stack stack = stackService.getById(payload.getResourceId());
                MDCBuilder.buildMdcContext(stack);
                flow.setFlowFailed(payload.getException());
                return DatabaseBackupContext.from(flowParameters, payload, null);
            }

            @Override
            protected void doExecute(DatabaseBackupContext context, DatabaseBackupFailedEvent payload, Map<Object, Object> variables) throws Exception {
                backupRestoreStatusService.handleDatabaseBackupFailure(context.getStackId(), payload.getException().getMessage(), payload.getDetailedStatus());
                sendEvent(context, DATABASE_BACKUP_FAILED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatabaseBackupFailedEvent payload, Optional<DatabaseBackupContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
