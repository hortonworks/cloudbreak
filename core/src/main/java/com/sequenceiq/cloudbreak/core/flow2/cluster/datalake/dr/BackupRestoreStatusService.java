package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_BACKUP;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_BACKUP_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_BACKUP_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_RESTORE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_RESTORE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_RESTORE_FINISHED;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Service
public class BackupRestoreStatusService {

    private static final String ERRORS_STRING = "Error(s): ";

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    public void backupDatabase(long stackId, String backupId) {
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.DATABASE_BACKUP_IN_PROGRESS, "Initiating database backup " + backupId);
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), DATALAKE_DATABASE_BACKUP);
    }

    public void backupDatabaseFinished(long stackId) {
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.DATABASE_BACKUP_FINISHED, "Database was successfully backed up.");
        flowMessageService.fireEventAndLog(stackId, Status.AVAILABLE.name(), DATALAKE_DATABASE_BACKUP_FINISHED);
    }

    public void handleDatabaseBackupFailure(long stackId, String errorReason, DetailedStackStatus detailedStatus) {
        stackUpdater.updateStackStatus(stackId, detailedStatus, extractSaltErrorIfAvailable(errorReason));
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_FAILED.name(), DATALAKE_DATABASE_BACKUP_FAILED, errorReason);
    }

    public void restoreDatabase(long stackId, String backupId) {
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.DATABASE_RESTORE_IN_PROGRESS, "Initiating database restore " + backupId);
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), DATALAKE_DATABASE_RESTORE);
    }

    public void restoreDatabaseFinished(long stackId) {
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.DATABASE_RESTORE_FINISHED, "Database was successfully restored.");
        flowMessageService.fireEventAndLog(stackId, Status.AVAILABLE.name(), DATALAKE_DATABASE_RESTORE_FINISHED);
    }

    public void handleDatabaseRestoreFailure(long stackId, String errorReason, DetailedStackStatus detailedStatus) {
        stackUpdater.updateStackStatus(stackId, detailedStatus, extractSaltErrorIfAvailable(errorReason));
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_FAILED.name(), DATALAKE_DATABASE_RESTORE_FAILED, errorReason);
    }

    private String extractSaltErrorIfAvailable(String errorReason) {
        if (errorReason.contains(ERRORS_STRING)) {
            return errorReason.substring(errorReason.indexOf(ERRORS_STRING) + ERRORS_STRING.length()).replaceAll("\n", "; ");
        }
        return errorReason;
    }
}
