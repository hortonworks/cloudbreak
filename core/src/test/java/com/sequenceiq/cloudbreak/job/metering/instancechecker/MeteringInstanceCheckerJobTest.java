package com.sequenceiq.cloudbreak.job.metering.instancechecker;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import org.quartz.JobExecutionException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.metering.MeteringInstanceCheckerService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class MeteringInstanceCheckerJobTest {

    private static final Long LOCAL_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private MeteringInstanceCheckerService meteringInstanceCheckerService;

    @Mock
    private MeteringInstanceCheckerJobService meteringInstanceCheckerJobService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private MeteringInstanceCheckerJob underTest;

    @BeforeEach
    public void setUp() {
        underTest.setLocalId(String.valueOf(LOCAL_ID));
    }

    @Test
    void testExecuteWhenClusterRunning() throws JobExecutionException {
        StackDto stack = stack(AVAILABLE);
        when(stackDtoService.getById(eq(LOCAL_ID))).thenReturn(stack);
        underTest.executeTracedJob(jobExecutionContext);
        verify(meteringInstanceCheckerJobService, never()).unschedule(eq(String.valueOf(LOCAL_ID)));
        verify(meteringInstanceCheckerService, times(1)).checkInstanceTypes(eq(stack));
    }

    @Test
    void testExecuteWhenClusterStopped() throws JobExecutionException {
        StackDto stack = stack(STOPPED);
        when(stackDtoService.getById(eq(LOCAL_ID))).thenReturn(stack);
        underTest.executeTracedJob(jobExecutionContext);
        verify(meteringInstanceCheckerJobService, times(1)).unschedule(eq(String.valueOf(LOCAL_ID)));
        verify(meteringInstanceCheckerService, never()).checkInstanceTypes(eq(stack));
    }

    private StackDto stack(Status status) {
        StackDto stack = mock(StackDto.class);
        when(stack.getStatus()).thenReturn(status);
        return stack;
    }
}