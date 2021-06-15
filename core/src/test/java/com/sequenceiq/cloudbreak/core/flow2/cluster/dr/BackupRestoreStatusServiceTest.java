package com.sequenceiq.cloudbreak.core.flow2.cluster.dr;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_BACKUP;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_BACKUP_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_BACKUP_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_RESTORE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_RESTORE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_RESTORE_FINISHED;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.BackupRestoreStatusService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BackupRestoreStatusServiceTest {

    private static final String ERROR_MESSAGE = "error message";

    private static final long STACK_ID = 1L;

    private static final String BACKUP_ID = "backup1";

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private BackupRestoreStatusService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testBackupStarted() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        service.backupDatabase(STACK_ID, BACKUP_ID);
        verify(clusterService, times(1)).updateClusterStatusByStackId(STACK_ID, Status.BACKUP_IN_PROGRESS);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_IN_PROGRESS.name()), captor.capture());
        assertEquals(DATALAKE_DATABASE_BACKUP, captor.getValue());
    }

    @Test
    public void testBackupFinished() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        service.backupDatabaseFinished(STACK_ID);
        verify(clusterService, times(1)).updateClusterStatusByStackId(STACK_ID, Status.AVAILABLE);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.AVAILABLE.name()), captor.capture());
        assertEquals(DATALAKE_DATABASE_BACKUP_FINISHED, captor.getValue());
    }

    @Test
    public void testBackupFailure() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        service.handleDatabaseBackupFailure(STACK_ID, ERROR_MESSAGE, DetailedStackStatus.DATABASE_BACKUP_FAILED);
        verify(clusterService, times(1)).updateClusterStatusByStackId(STACK_ID, Status.AVAILABLE, ERROR_MESSAGE);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_FAILED.name()), captor.capture(), eq(ERROR_MESSAGE));
        assertEquals(DATALAKE_DATABASE_BACKUP_FAILED, captor.getValue());
    }

    @Test
    public void testRestoreStarted() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        service.restoreDatabase(STACK_ID, BACKUP_ID);
        verify(clusterService, times(1)).updateClusterStatusByStackId(STACK_ID, Status.RESTORE_IN_PROGRESS);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_IN_PROGRESS.name()), captor.capture());
        assertEquals(DATALAKE_DATABASE_RESTORE, captor.getValue());
    }

    @Test
    public void testRestoreFinished() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        service.restoreDatabaseFinished(STACK_ID);
        verify(clusterService, times(1)).updateClusterStatusByStackId(STACK_ID, Status.AVAILABLE);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.AVAILABLE.name()), captor.capture());
        assertEquals(DATALAKE_DATABASE_RESTORE_FINISHED, captor.getValue());
    }

    @Test
    public void testRestorepFailure() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        service.handleDatabaseRestoreFailure(STACK_ID, ERROR_MESSAGE, DetailedStackStatus.DATABASE_RESTORE_FAILED);
        verify(clusterService, times(1)).updateClusterStatusByStackId(STACK_ID, Status.AVAILABLE, ERROR_MESSAGE);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_FAILED.name()), captor.capture(), eq(ERROR_MESSAGE));
        assertEquals(DATALAKE_DATABASE_RESTORE_FAILED, captor.getValue());
    }
}
