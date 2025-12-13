package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.BackupRestoreStatusService.ERRORS_STRING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_BACKUP;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_BACKUP_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_BACKUP_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_RESTORE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_RESTORE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_RESTORE_FINISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@ExtendWith(MockitoExtension.class)
class BackupRestoreStatusServiceTest {

    private static final String ERROR_MESSAGE = "error message";

    private static final String RAW_STDERR = ERRORS_STRING + "Mar 04, 2022 6:02:29 PM org.apache.knox.gateway.shell.KnoxSession " +
            "createClient\nINFO: Using default JAAS configuration\nmkdir: hreeve-dev3/denied/backup1_database_backup: PUT 0-byte " +
            "object  on hreeve-dev3/denied/backup1_database_backup: com.amazonaws.services.s3.model.AmazonS3Exception: Access Denied " +
            "(Service: Amazon S3; Status Code: 403; Error Code: AccessDenied; Request ID: AZWVXNAMVZGRMH6N; S3 Extended Request ID: " +
            "+kQvwLsrvKvWJ8qXD6ffatnrAl6ktRwt7kNmv3CeF4rGLPVMLO5iKHShXR9SeA99apTunuMFHPk=; Proxy: null), S3 Extended Request ID: " +
            "+kQvwLsrvKvWJ8qXD6ffatnrAl6ktRwt7kNmv3CeF4rGLPVMLO5iKHShXR9SeA99apTunuMFHPk=:AccessDenied\nMar 04, 2022 6:02:32 PM org." +
            "apache.knox.gateway.shell.KnoxSession createClient\nINFO: Using default JAAS configuration\nmoveFromLocal: `s3a://eng-sdx-" +
            "daily-v2-datalake/hreeve-dev3/denied/backup1_database_backup/hive_backup': No such file or directory";

    private static final String PARSED_STDERR = "mkdir: hreeve-dev3/denied/backup1_database_backup: PUT 0-byte object  on hreeve-dev3/" +
            "denied/backup1_database_backup: com.amazonaws.services.s3.model.AmazonS3Exception: Access Denied (Service: Amazon S3; Status " +
            "Code: 403; Error Code: AccessDenied; Request ID: AZWVXNAMVZGRMH6N; S3 Extended Request ID: +kQvwLsrvKvWJ8qXD6ffatnrAl6ktRwt7k" +
            "Nmv3CeF4rGLPVMLO5iKHShXR9SeA99apTunuMFHPk=; Proxy: null), S3 Extended Request ID: +kQvwLsrvKvWJ8qXD6ffatnrAl6ktRwt7kNmv3CeF4r" +
            "GLPVMLO5iKHShXR9SeA99apTunuMFHPk=:AccessDenied; moveFromLocal: `s3a://eng-sdx-daily-v2-datalake/hreeve-dev3/denied/backup1_" +
            "database_backup/hive_backup': No such file or directory; ";

    private static final long STACK_ID = 1L;

    private static final String BACKUP_ID = "backup1";

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private BackupRestoreStatusService service;

    @Test
    void testBackupStarted() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        service.backupDatabase(STACK_ID, BACKUP_ID, false);
        verify(stackUpdater, times(1)).updateStackStatus(STACK_ID, DetailedStackStatus.DATABASE_BACKUP_IN_PROGRESS,
                "Initiating database backup backup1");
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_IN_PROGRESS.name()), captor.capture());
        assertEquals(DATALAKE_DATABASE_BACKUP, captor.getValue());
    }

    @Test
    void testBackupFinished() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        service.backupDatabaseFinished(STACK_ID, false);
        verify(stackUpdater, times(1)).updateStackStatus(STACK_ID, DetailedStackStatus.DATABASE_BACKUP_FINISHED,
                "Database was successfully backed up. Continuing with the rest.");
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.AVAILABLE.name()), captor.capture());
        assertEquals(DATALAKE_DATABASE_BACKUP_FINISHED, captor.getValue());
    }

    @Test
    void testBackupFailure() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        service.handleDatabaseBackupFailure(STACK_ID, ERROR_MESSAGE, DetailedStackStatus.DATABASE_BACKUP_FAILED, false);
        verify(stackUpdater, times(1)).updateStackStatus(STACK_ID, DetailedStackStatus.DATABASE_BACKUP_FAILED, ERROR_MESSAGE);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_FAILED.name()), captor.capture(), eq(ERROR_MESSAGE));
        assertEquals(DATALAKE_DATABASE_BACKUP_FAILED, captor.getValue());
    }

    @Test
    void testRestoreStarted() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        service.restoreDatabase(STACK_ID, BACKUP_ID, false);
        verify(stackUpdater, times(1)).updateStackStatus(STACK_ID, DetailedStackStatus.DATABASE_RESTORE_IN_PROGRESS,
                "Initiating database restore backup1");
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_IN_PROGRESS.name()), captor.capture());
        assertEquals(DATALAKE_DATABASE_RESTORE, captor.getValue());
    }

    @Test
    void testRestoreFinished() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        service.restoreDatabaseFinished(STACK_ID, false);
        verify(stackUpdater, times(1)).updateStackStatus(STACK_ID, DetailedStackStatus.DATABASE_RESTORE_FINISHED,
                "Database was successfully restored. Continuing with the rest.");
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.AVAILABLE.name()), captor.capture());
        assertEquals(DATALAKE_DATABASE_RESTORE_FINISHED, captor.getValue());
    }

    @Test
    void testRestoreFailure() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        service.handleDatabaseRestoreFailure(STACK_ID, ERROR_MESSAGE, DetailedStackStatus.DATABASE_RESTORE_FAILED, false);
        verify(stackUpdater, times(1)).updateStackStatus(STACK_ID, DetailedStackStatus.DATABASE_RESTORE_FAILED, ERROR_MESSAGE);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_FAILED.name()), captor.capture(), eq(ERROR_MESSAGE));
        assertEquals(DATALAKE_DATABASE_RESTORE_FAILED, captor.getValue());
    }

    @Test
    void testBackupFailureParseStderr() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        service.handleDatabaseBackupFailure(STACK_ID, RAW_STDERR, DetailedStackStatus.DATABASE_BACKUP_FAILED, false);
        verify(stackUpdater, times(1)).updateStackStatus(STACK_ID, DetailedStackStatus.DATABASE_BACKUP_FAILED, PARSED_STDERR);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_FAILED.name()), captor.capture(), eq(RAW_STDERR));
        assertEquals(DATALAKE_DATABASE_BACKUP_FAILED, captor.getValue());
    }

    @Test
    void testRestoreFailureParseStderr() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        service.handleDatabaseRestoreFailure(STACK_ID, RAW_STDERR, DetailedStackStatus.DATABASE_RESTORE_FAILED, false);
        verify(stackUpdater, times(1)).updateStackStatus(STACK_ID, DetailedStackStatus.DATABASE_RESTORE_FAILED, PARSED_STDERR);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_FAILED.name()), captor.capture(), eq(RAW_STDERR));
        assertEquals(DATALAKE_DATABASE_RESTORE_FAILED, captor.getValue());
    }
}
