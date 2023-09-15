package com.sequenceiq.cloudbreak.job.metering;

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
class MeteringJobInitializerTest {

    @Mock
    private StackService stackService;

    @Mock
    private MeteringJobService meteringJobService;

    @Mock
    private MeteringConfig meteringConfig;

    @InjectMocks
    private MeteringJobInitializer underTest;

    @Test
    void testInitJobsWithAliveDatahubs() {
        when(meteringConfig.isEnabled()).thenReturn(Boolean.TRUE);
        JobResource jobResource1 = mock(JobResource.class);
        JobResource jobResource2 = mock(JobResource.class);
        when(stackService.getAllAliveDatahubs(anySet())).thenReturn(List.of(jobResource1, jobResource2));
        underTest.initJobs();
        verify(meteringJobService, times(2)).schedule(any());
    }

    @Test
    void testInitJobsWithoutAliveDatahubs() {
        when(meteringConfig.isEnabled()).thenReturn(Boolean.TRUE);
        when(stackService.getAllAliveDatahubs(anySet())).thenReturn(List.of());
        underTest.initJobs();
        verify(meteringJobService, never()).schedule(any());
    }
}