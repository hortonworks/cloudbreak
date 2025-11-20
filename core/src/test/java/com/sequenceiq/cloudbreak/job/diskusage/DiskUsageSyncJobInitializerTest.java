package com.sequenceiq.cloudbreak.job.diskusage;

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

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class DiskUsageSyncJobInitializerTest {

    @Mock
    private StackService stackService;

    @Mock
    private DiskUsageSyncJobService diskUsageSyncJobService;

    @Mock
    private DiskUsageSyncConfig diskUsageSyncConfig;

    @InjectMocks
    private DiskUsageSyncJobInitializer underTest;

    @Test
    void testInitJobsWithAliveDatahubs() {
        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(Boolean.TRUE);
        JobResource jobResource1 = mock(JobResource.class);
        JobResource jobResource2 = mock(JobResource.class);
        JobResource jobResource3 = mock(JobResource.class);
        when(stackService.getAllWhereStatusNotIn(anySet())).thenReturn(List.of(jobResource1, jobResource2, jobResource3));

        underTest.initJobs();

        verify(diskUsageSyncJobService, times(3)).schedule((DiskUsageSyncJobAdapter) any());
    }

    @Test
    void testInitJobsWithoutAliveDatahubs() {
        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(Boolean.TRUE);
        when(stackService.getAllWhereStatusNotIn(anySet())).thenReturn(List.of());

        underTest.initJobs();

        verify(diskUsageSyncJobService, never()).schedule((DiskUsageSyncJobAdapter) any());
    }

    @Test
    void testInitJobsWhenProviderSyncIsDisabled() {
        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(Boolean.FALSE);

        underTest.initJobs();

        verify(stackService, never()).getAllWhereStatusNotIn(anySet());
        verify(diskUsageSyncJobService, never()).schedule((DiskUsageSyncJobAdapter) any());
    }
}