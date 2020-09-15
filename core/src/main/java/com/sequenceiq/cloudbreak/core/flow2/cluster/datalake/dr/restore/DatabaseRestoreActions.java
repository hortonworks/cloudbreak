package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.RESTORE_FAIL_HANDLED_EVENT;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.AbstractBackupRestoreActions;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.BackupRestoreContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.BackupRestoreStatusService;
import com.sequenceiq.cloudbreak.core.flow2.event.DatabaseRestoreTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.FullRestoreInProgressEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.FullRestoreStatusRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.DatalakeRestoreFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.DatabaseRestoreRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.DatalakeRestoreSuccess;
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
public class DatabaseRestoreActions {

    @Inject
    private BackupRestoreStatusService backupRestoreStatusService;

    @Bean(name = "DATABASE_RESTORE_IN_PROGRESS_STATE")
    public Action<?, ?> restoreDatabase() {
        return new AbstractBackupRestoreActions<>(DatabaseRestoreTriggerEvent.class) {

            @Override
            protected void doExecute(BackupRestoreContext context, DatabaseRestoreTriggerEvent payload, Map<Object, Object> variables) {
                backupRestoreStatusService.restoreDatabase(context.getStackId(), context.getBackupId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(BackupRestoreContext context) {
                return new DatabaseRestoreRequest(context.getStackId(), context.getBackupLocation(),
                    context.getBackupId(), context.getUserCrn());
            }

            @Override
            protected Object getFailurePayload(DatabaseRestoreTriggerEvent payload, Optional<BackupRestoreContext> flowContext, Exception ex) {
                return DatalakeRestoreFailedEvent.from(payload, ex, DetailedStackStatus.DATABASE_RESTORE_FAILED);
            }
        };
    }

    @Bean(name = "FULL_RESTORE_IN_PROGRESS_STATE")
    public Action<?, ?> checkForFullRestoreStatus() {
        return new AbstractBackupRestoreActions<>(FullRestoreInProgressEvent.class) {

            @Override
            protected void doExecute(BackupRestoreContext context, FullRestoreInProgressEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(BackupRestoreContext context) {
                return new FullRestoreStatusRequest(context.getStackId(), context.getBackupId(), context.getUserCrn());
            }

            @Override
            protected Object getFailurePayload(FullRestoreInProgressEvent payload, Optional<BackupRestoreContext> flowContext, Exception ex) {
                return DatalakeRestoreFailedEvent.from(payload, ex, DetailedStackStatus.FULL_RESTORE_FAILED);
            }
        };
    }

    @Bean(name = "RESTORE_FINISHED_STATE")
    public Action<?, ?> datalakeRestoreFinished() {
        return new AbstractBackupRestoreActions<>(DatalakeRestoreSuccess.class) {

            @Override
            protected void doExecute(BackupRestoreContext context, DatalakeRestoreSuccess payload, Map<Object, Object> variables) {
                backupRestoreStatusService.restoreDatabaseFinished(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(BackupRestoreContext context) {
                return new StackEvent(DatabaseRestoreEvent.RESTORE_FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "RESTORE_FAILED_STATE")
    public Action<?, ?> datalakeRestoreFailedAction() {
        return new AbstractBackupRestoreActions<>(DatalakeRestoreFailedEvent.class) {

            @Override
            protected BackupRestoreContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeRestoreFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return BackupRestoreContext.from(flowParameters, payload, null, null, null);
            }

            @Override
            protected void doExecute(BackupRestoreContext context, DatalakeRestoreFailedEvent payload, Map<Object, Object> variables) {
                backupRestoreStatusService.handleDatabaseRestoreFailure(context.getStackId(), payload.getException().getMessage(), payload.getDetailedStatus());
                sendEvent(context, RESTORE_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
