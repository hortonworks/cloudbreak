package com.sequenceiq.cloudbreak.job.metering.sync;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.metering.MeteringService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class MeteringSyncJobTest {

    private static final Long LOCAL_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private MeteringService meteringService;

    @Mock
    private MeteringSyncJobService meteringJobService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private MeteringSyncJob underTest;

    @BeforeEach
    public void setUp() {
        underTest.setLocalId(String.valueOf(LOCAL_ID));
    }

    @Test
    void testExecuteWhenClusterRunning() {
        StackView stack = stack(DetailedStackStatus.AVAILABLE);
        when(stackDtoService.getStackViewById(eq(LOCAL_ID))).thenReturn(stack);
        underTest.executeJob(jobExecutionContext);
        verify(meteringJobService, never()).unschedule(eq(String.valueOf(LOCAL_ID)));
        verify(meteringService, times(1)).sendMeteringSyncEventForStack(eq(LOCAL_ID));
    }

    @Test
    void testExecuteWhenClusterStopped() {
        StackView stack = stack(DetailedStackStatus.STOPPED);
        when(stackDtoService.getStackViewById(eq(LOCAL_ID))).thenReturn(stack);
        underTest.executeJob(jobExecutionContext);
        verify(meteringJobService, times(1)).unschedule(eq(String.valueOf(LOCAL_ID)));
        verify(meteringService, never()).sendMeteringSyncEventForStack(eq(LOCAL_ID));
    }

    @Test
    void testExecuteWhenClusterDeletedOnProviderSide() {
        StackView stack = stack(DetailedStackStatus.DELETED_ON_PROVIDER_SIDE);
        when(stackDtoService.getStackViewById(eq(LOCAL_ID))).thenReturn(stack);
        underTest.executeJob(jobExecutionContext);
        verify(meteringJobService, never()).unschedule(eq(String.valueOf(LOCAL_ID)));
        verify(meteringService, never()).sendMeteringSyncEventForStack(eq(LOCAL_ID));
    }

    private Stack stack(DetailedStackStatus detailedStackStatus) {
        Stack stack = new Stack();
        stack.setId(LOCAL_ID);
        stack.setStackStatus(new StackStatus(stack, detailedStackStatus));
        return stack;
    }
}