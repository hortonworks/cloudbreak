package com.sequenceiq.redbeams.sync.provider;

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

import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class RdsProviderSyncJobTest {

    private static final Long LOCAL_ID = 1L;

    @Mock
    private DBStackService dbStackService;

    @Mock
    private RdsProviderSyncService rdsProviderSyncService;

    @Mock
    private RdsProviderSyncJobService jobService;

    @Mock
    private RdsProviderSyncConfig config;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private DBStack dbStack;

    @InjectMocks
    private RdsProviderSyncJob underTest;

    @BeforeEach
    void setUp() {
        underTest.setLocalId(String.valueOf(LOCAL_ID));
        when(dbStackService.getById(eq(LOCAL_ID))).thenReturn(dbStack);
    }

    @Test
    void shouldNotRunWhenDisabled() {
        when(dbStack.getStatus()).thenReturn(Status.AVAILABLE);
        when(config.isEnabled()).thenReturn(false);

        underTest.executeJob(jobExecutionContext);

        verify(rdsProviderSyncService, never()).syncInstanceTypeAndVersion(any());
        verify(jobService, never()).deregister(any());
    }

    @Test
    void shouldNotRunWhenFlowRunning() {
        when(dbStack.getStatus()).thenReturn(Status.AVAILABLE);
        when(config.isEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(eq(LOCAL_ID))).thenReturn(true);

        underTest.executeJob(jobExecutionContext);

        verify(rdsProviderSyncService, never()).syncInstanceTypeAndVersion(any());
        verify(jobService, never()).deregister(any());
    }

    @Test
    void shouldDeregisterWhenDeleteCompleted() {
        when(dbStack.getStatus()).thenReturn(Status.DELETE_COMPLETED);
        when(config.isEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(eq(LOCAL_ID))).thenReturn(false);
        when(jobExecutionContext.getJobDetail()).thenReturn(new JobDetailImpl());

        underTest.executeJob(jobExecutionContext);

        verify(rdsProviderSyncService, never()).syncInstanceTypeAndVersion(any());
        verify(jobService, times(1)).deregister(any());
    }

    @Test
    void shouldSyncWhenAvailable() {
        when(dbStack.getStatus()).thenReturn(Status.AVAILABLE);
        when(config.isEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(eq(LOCAL_ID))).thenReturn(false);

        underTest.executeJob(jobExecutionContext);

        verify(rdsProviderSyncService, times(1)).syncInstanceTypeAndVersion(eq(dbStack));
        verify(jobService, never()).deregister(any());
    }

    @Test
    void shouldNotSyncWhenNotAvailable() {
        when(dbStack.getStatus()).thenReturn(Status.STOPPED);
        when(config.isEnabled()).thenReturn(true);
        when(flowLogService.isOtherFlowRunning(eq(LOCAL_ID))).thenReturn(false);

        underTest.executeJob(jobExecutionContext);

        verify(rdsProviderSyncService, never()).syncInstanceTypeAndVersion(any());
        verify(jobService, never()).deregister(any());
    }
}
