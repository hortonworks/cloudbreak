package com.sequenceiq.redbeams.sync;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;

import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
public class DBStackStatusSyncJobTest {

    private static final String JOB_LOCAL_ID = "1234";

    private static final long DB_STACK_ID = 1234L;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private DBStackService dbStackService;

    @Mock
    private DBStackStatusSyncService dbStackStatusSyncService;

    @Mock
    private DBStackJobService dbStackJobService;

    @Mock
    private StatusCheckerJobService jobService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private DBStack dbStack;

    @InjectMocks
    private DBStackStatusSyncJob victim;

    @BeforeEach
    public void initTests() {
        MockitoAnnotations.initMocks(this);
        victim.setLocalId(JOB_LOCAL_ID);

        when(dbStackService.getById(DB_STACK_ID)).thenReturn(dbStack);
    }

    @Test
    public void shouldNotCallSyncWhenOtherFlowIsRunning() {
        when(dbStack.getStatus()).thenReturn(Status.AVAILABLE);
        when(flowLogService.isOtherFlowRunning(DB_STACK_ID)).thenReturn(true);

        victim.executeJob(jobExecutionContext);

        verifyNoInteractions(dbStackStatusSyncService);
    }

    @Test
    public void shouldCallSync() {
        when(dbStack.getStatus()).thenReturn(Status.AVAILABLE);
        when(flowLogService.isOtherFlowRunning(DB_STACK_ID)).thenReturn(false);

        victim.executeJob(jobExecutionContext);

        verify(dbStackStatusSyncService).sync(dbStack);
    }

    @Test
    public void shouldSwitchToLongSyncWhenDeletedOnProviderSide() {
        when(dbStack.getStatus()).thenReturn(Status.DELETED_ON_PROVIDER_SIDE);
        when(jobService.isLongSyncJob(jobExecutionContext)).thenReturn(false);
        when(flowLogService.isOtherFlowRunning(DB_STACK_ID)).thenReturn(false);

        victim.executeJob(jobExecutionContext);

        verify(dbStackJobService).scheduleLongIntervalCheck(DB_STACK_ID);
        verify(dbStackStatusSyncService).sync(dbStack);
    }

    @Test
    public void shouldSwitchToShortSyncWhenRecovered() {
        when(dbStack.getStatus()).thenReturn(Status.AVAILABLE);
        when(jobService.isLongSyncJob(jobExecutionContext)).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(DB_STACK_ID)).thenReturn(false);

        victim.executeJob(jobExecutionContext);

        verify(dbStackJobService).schedule(DB_STACK_ID);
        verify(dbStackStatusSyncService).sync(dbStack);
    }

    @Test
    public void shouldNotRescheduleWhenAlreadyOnLongSync() {
        when(dbStack.getStatus()).thenReturn(Status.DELETED_ON_PROVIDER_SIDE);
        when(jobService.isLongSyncJob(jobExecutionContext)).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(DB_STACK_ID)).thenReturn(false);

        victim.executeJob(jobExecutionContext);

        verifyNoInteractions(dbStackJobService);
        verify(dbStackStatusSyncService).sync(dbStack);
    }

    @Test
    public void shouldNotRescheduleWhenAlreadyOnShortSync() {
        when(dbStack.getStatus()).thenReturn(Status.AVAILABLE);
        when(jobService.isLongSyncJob(jobExecutionContext)).thenReturn(false);
        when(flowLogService.isOtherFlowRunning(DB_STACK_ID)).thenReturn(false);

        victim.executeJob(jobExecutionContext);

        verifyNoInteractions(dbStackJobService);
        verify(dbStackStatusSyncService).sync(dbStack);
    }
}
