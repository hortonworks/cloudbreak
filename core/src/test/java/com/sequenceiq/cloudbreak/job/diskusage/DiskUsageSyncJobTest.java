package com.sequenceiq.cloudbreak.job.diskusage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.impl.JobDetailImpl;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(MockitoExtension.class)
class DiskUsageSyncJobTest {

    private static final Long LOCAL_ID = 1L;

    private static final String ACCOUNT_ID = "account-id";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private DiskUsageSyncService diskUsageSyncService;

    @Mock
    private DiskUsageSyncJobService diskUsageSyncJobService;

    @Mock
    private DiskUsageSyncConfig diskUsageSyncConfig;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private StackDto stackDto;

    @InjectMocks
    private DiskUsageSyncJob underTest;

    @BeforeEach
    public void setUp() {
        underTest.setLocalId(String.valueOf(LOCAL_ID));
        when(stackDtoService.getById(eq(LOCAL_ID))).thenReturn(stackDto);
    }

    @Test
    void testExecuteWhenProviderSyncDisabled() {
        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(false);

        underTest.executeJob(jobExecutionContext);

        verify(diskUsageSyncService, never()).checkDbDisk(any());
        verify(diskUsageSyncJobService, never()).deregister(any());
    }

    @Test
    void testExecuteWhenFlowIsRunning() {
        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(eq(LOCAL_ID))).thenReturn(true);

        underTest.executeJob(jobExecutionContext);

        verify(diskUsageSyncService, never()).checkDbDisk(any());
        verify(diskUsageSyncJobService, never()).deregister(any());
    }

    @Test
    void testExecuteWhenStackStatusUnschedulable() {

        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(eq(LOCAL_ID))).thenReturn(false);
        when(stackDto.getStatus()).thenReturn(Status.DELETE_COMPLETED);
        when(jobExecutionContext.getJobDetail()).thenReturn(new JobDetailImpl());

        underTest.executeJob(jobExecutionContext);

        verify(diskUsageSyncService, never()).checkDbDisk(any());
        verify(diskUsageSyncJobService, times(1)).deregister(any());
    }

    @Test
    void testExecuteWhenStackStatusAvailable() {

        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(eq(LOCAL_ID))).thenReturn(false);
        when(stackDto.getStatus()).thenReturn(Status.AVAILABLE);
        when(stackDto.getAccountId()).thenReturn(ACCOUNT_ID);

        underTest.executeJob(jobExecutionContext);

        verify(diskUsageSyncService, times(1)).checkDbDisk(eq(stackDto));
        verify(diskUsageSyncJobService, never()).deregister(any());
    }

    @Test
    void testExecuteWhenStackStatusNotAvailable() {
        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(eq(LOCAL_ID))).thenReturn(false);
        when(stackDto.getStatus()).thenReturn(Status.STOPPED);

        underTest.executeJob(jobExecutionContext);

        verify(diskUsageSyncService, never()).checkDbDisk(any());
        verify(diskUsageSyncJobService, never()).deregister(any());
    }
}