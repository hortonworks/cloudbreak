package com.sequenceiq.cloudbreak.job.disk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.quartz.JobExecutionException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(MockitoExtension.class)
public class DiskSyncJobTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackDtoService stackService;

    @Mock
    private DiskSyncConfig config;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private DiskSyncJobService jobService;

    @Mock
    private DiskSyncService diskSyncService;

    @InjectMocks
    private DiskSyncJob underTest;

    @BeforeEach
    public void setUp() {
        underTest.setLocalId(String.valueOf(STACK_ID));
    }

    @Test
    public void testExecuteJobWhenDiskSyncIsDisabled() throws JobExecutionException {
        when(config.isDiskSyncEnabled()).thenReturn(false);

        underTest.executeJob(mock(JobExecutionContext.class));

        verify(diskSyncService, times(0)).syncResources(any(), any(DiskSyncMode.class));
    }

    @Test
    public void testExecuteJobWhenFlowIsRunning() throws JobExecutionException {
        when(config.isDiskSyncEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(STACK_ID)).thenReturn(true);
        StackDto stack = mock(StackDto.class);
        when(stackService.getById(STACK_ID)).thenReturn(stack);

        underTest.executeJob(mock(JobExecutionContext.class));

        verify(diskSyncService, times(0)).syncResources(stack, DiskSyncMode.DRY_RUN);
    }

    @Test
    public void testExecuteJobWhenStackIsAvailable() throws JobExecutionException {
        when(config.isDiskSyncEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(STACK_ID)).thenReturn(false);
        StackDto stack = mock(StackDto.class);
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        when(stackService.getById(STACK_ID)).thenReturn(stack);

        underTest.executeJob(mock(JobExecutionContext.class));

        verify(diskSyncService, times(1)).syncResources(stack, DiskSyncMode.DRY_RUN);
    }

    @Test
    public void testExecuteJobWhenStackIsNotAvailable() throws JobExecutionException {
        when(config.isDiskSyncEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(STACK_ID)).thenReturn(false);
        StackDto stack = mock(StackDto.class);
        when(stack.getStatus()).thenReturn(Status.CREATE_IN_PROGRESS);
        when(stackService.getById(STACK_ID)).thenReturn(stack);

        underTest.executeJob(mock(JobExecutionContext.class));

        verify(diskSyncService, times(0)).syncResources(stack, DiskSyncMode.DRY_RUN);
    }
}
