package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.DATABASE_RESTORE_FAIL_HANDLED_EVENT;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.AbstractBackupRestoreActions;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.BackupRestoreContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.BackupRestoreStatusService;
import com.sequenceiq.cloudbreak.core.flow2.event.DatabaseRestoreTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.DatabaseRestoreFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.DatabaseRestoreRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.DatabaseRestoreSuccess;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class DatabaseRestoreActions {

    @Inject
    private BackupRestoreStatusService backupRestoreStatusService;

    @Bean(name = "DATABASE_RESTORE_STATE")
    public Action<?, ?> restoreDatabase() {
        return new AbstractBackupRestoreActions<>(DatabaseRestoreTriggerEvent.class) {

            @Override
            protected void doExecute(BackupRestoreContext context, DatabaseRestoreTriggerEvent payload, Map<Object, Object> variables) {
                backupRestoreStatusService.restoreDatabase(context.getStackId(), context.getBackupId(), context.isDryRun());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(BackupRestoreContext context) {
                return new DatabaseRestoreRequest(context.getStackId(), context.getBackupLocation(), context.getBackupId(),
                        context.getDatabaseMaxDurationInMin(), context.isDryRun());
            }

            @Override
            protected Object getFailurePayload(DatabaseRestoreTriggerEvent payload, Optional<BackupRestoreContext> flowContext, Exception ex) {
                return DatabaseRestoreFailedEvent.from(payload, ex, DetailedStackStatus.DATABASE_RESTORE_FAILED);
            }
        };
    }

    @Bean(name = "DATABASE_RESTORE_FINISHED_STATE")
    public Action<?, ?> databaseRestoreFinished() {
        return new AbstractBackupRestoreActions<>(DatabaseRestoreSuccess.class) {

            @Override
            protected void doExecute(BackupRestoreContext context, DatabaseRestoreSuccess payload, Map<Object, Object> variables) {
                backupRestoreStatusService.restoreDatabaseFinished(context.getStackId(), context.isDryRun());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(BackupRestoreContext context) {
                return new StackEvent(DatabaseRestoreEvent.DATABASE_RESTORE_FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "DATABASE_RESTORE_FAILED_STATE")
    public Action<?, ?> databaseRestoreFailedAction() {
        return new AbstractBackupRestoreActions<>(DatabaseRestoreFailedEvent.class) {

            @Override
            protected BackupRestoreContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatabaseRestoreFailedEvent payload) {
                return BackupRestoreContext.from(flowParameters, payload, null, null, true, payload.getSkipDatabaseNames(),
                        payload.getDatabaseMaxDurationInMin(), payload.isDryRun());
            }

            @Override
            protected void doExecute(BackupRestoreContext context, DatabaseRestoreFailedEvent payload, Map<Object, Object> variables) {
                backupRestoreStatusService.handleDatabaseRestoreFailure(context.getStackId(), payload.getException().getMessage(), payload.getDetailedStatus(),
                    payload.isDryRun());
                sendEvent(context, DATABASE_RESTORE_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
