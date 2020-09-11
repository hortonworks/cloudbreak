package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.BACKUP_FAIL_HANDLED_EVENT;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.AbstractBackupRestoreActions;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.BackupRestoreContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.BackupRestoreStatusService;
import com.sequenceiq.cloudbreak.core.flow2.event.DatabaseBackupTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.DatabaseBackupFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.DatabaseBackupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.BackupSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.FullBackupInProgressEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.FullBackupStatusRequest;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

@Configuration
public class DatabaseBackupActions {

    @Inject
    private BackupRestoreStatusService backupRestoreStatusService;

    @Bean(name = "DATABASE_BACKUP_IN_PROGRESS_STATE")
    public Action<?, ?> backupDatabase() {
        return new AbstractBackupRestoreActions<>(DatabaseBackupTriggerEvent.class) {

            @Override
            protected void doExecute(BackupRestoreContext context, DatabaseBackupTriggerEvent payload, Map<Object, Object> variables) {
                backupRestoreStatusService.backupDatabase(context.getStackId(), context.getBackupId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(BackupRestoreContext context) {
                return new DatabaseBackupRequest(context.getStackId(), context.getBackupLocation(),
                    context.getBackupId(), context.getUserCrn());
            }

            @Override
            protected Object getFailurePayload(DatabaseBackupTriggerEvent payload, Optional<BackupRestoreContext> flowContext, Exception ex) {
                return DatabaseBackupFailedEvent.from(payload, ex, DetailedStackStatus.DATABASE_BACKUP_FAILED);
            }
        };
    }

    @Bean(name = "FULL_BACKUP_IN_PROGRESS_STATE")
    public Action<?, ?> checkForFullBackupStatus() {
        return new AbstractBackupRestoreActions<>(FullBackupInProgressEvent.class) {

            @Override
            protected void doExecute(BackupRestoreContext context, FullBackupInProgressEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(BackupRestoreContext context) {
                return new FullBackupStatusRequest(context.getStackId(), context.getBackupId(), context.getUserCrn());
            }

            @Override
            protected Object getFailurePayload(FullBackupInProgressEvent payload, Optional<BackupRestoreContext> flowContext, Exception ex) {
                // TODO change detailed status
                return DatabaseBackupFailedEvent.from(payload, ex, DetailedStackStatus.DATABASE_BACKUP_FAILED);
            }
        };
    }

    @Bean(name = "BACKUP_FINISHED_STATE")
    public Action<?, ?> databaseBackupFinished() {
        return new AbstractBackupRestoreActions<>(BackupSuccess.class) {

            @Override
            protected void doExecute(BackupRestoreContext context, BackupSuccess payload, Map<Object, Object> variables) {
                backupRestoreStatusService.backupDatabaseFinished(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(BackupRestoreContext context) {
                return new StackEvent(DatabaseBackupEvent.BACKUP_FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "DATABASE_BACKUP_FAILED_STATE")
    public Action<?, ?> databaseBackupFailedAction() {
        return new AbstractBackupRestoreActions<>(DatabaseBackupFailedEvent.class) {

            @Override
            protected BackupRestoreContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatabaseBackupFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return BackupRestoreContext.from(flowParameters, payload, null, null, null);
            }

            @Override
            protected void doExecute(BackupRestoreContext context, DatabaseBackupFailedEvent payload, Map<Object, Object> variables) {
                backupRestoreStatusService.handleDatabaseBackupFailure(context.getStackId(), payload.getException().getMessage(), payload.getDetailedStatus());
                sendEvent(context, BACKUP_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
