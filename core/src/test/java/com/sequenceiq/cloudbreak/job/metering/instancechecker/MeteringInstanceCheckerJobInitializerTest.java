package com.sequenceiq.cloudbreak.job.metering.instancechecker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.metering.config.MeteringConfig;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class MeteringInstanceCheckerJobInitializerTest {

    @Mock
    private StackService stackService;

    @Mock
    private MeteringInstanceCheckerJobService meteringInstanceCheckerJobService;

    @Mock
    private MeteringConfig meteringConfig;

    @InjectMocks
    private MeteringInstanceCheckerJobInitializer underTest;

    @Test
    void testInitJobsWithAliveDatahubs() {
        when(meteringConfig.isEnabled()).thenReturn(Boolean.TRUE);
        when(meteringConfig.isInstanceCheckerEnabled()).thenReturn(Boolean.TRUE);
        JobResource jobResource1 = mock(JobResource.class);
        JobResource jobResource2 = mock(JobResource.class);
        when(stackService.getAllAliveDatahubs(anySet())).thenReturn(List.of(jobResource1, jobResource2));
        underTest.initJobs();
        verify(meteringInstanceCheckerJobService, times(2)).schedule(any());
    }

    @Test
    void testInitJobsWithoutAliveDatahubs() {
        when(meteringConfig.isEnabled()).thenReturn(Boolean.TRUE);
        when(meteringConfig.isInstanceCheckerEnabled()).thenReturn(Boolean.TRUE);
        when(stackService.getAllAliveDatahubs(anySet())).thenReturn(List.of());
        underTest.initJobs();
        verify(meteringInstanceCheckerJobService, never()).schedule(any());
    }

    @Test
    void testInitJobsWithAliveDatahubsWhenMeteringDisabled() {
        when(meteringConfig.isEnabled()).thenReturn(Boolean.FALSE);
        underTest.initJobs();
        verify(meteringInstanceCheckerJobService, never()).schedule(any());
    }

    @Test
    void testInitJobsWithAliveDatahubsWhenInstanceCheckerDisabled() {
        when(meteringConfig.isEnabled()).thenReturn(Boolean.TRUE);
        when(meteringConfig.isInstanceCheckerEnabled()).thenReturn(Boolean.FALSE);
        underTest.initJobs();
        verify(meteringInstanceCheckerJobService, never()).schedule(any());
    }
}