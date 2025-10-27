package com.sequenceiq.cloudbreak.job.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class ProviderSyncJobInitializerTest {

    @Mock
    private StackService stackService;

    @Mock
    private ProviderSyncJobService providerSyncJobService;

    @Mock
    private ProviderSyncConfig providerSyncConfig;

    @InjectMocks
    private ProviderSyncJobInitializer underTest;

    @Test
    void testInitJobsWithAliveDatahubs() {
        when(providerSyncConfig.isProviderSyncEnabled()).thenReturn(Boolean.TRUE);
        JobResource jobResource1 = mock(JobResource.class);
        JobResource jobResource2 = mock(JobResource.class);
        JobResource jobResource3 = mock(JobResource.class);
        when(providerSyncConfig.getEnabledProviders()).thenReturn(Set.of("AWS", "AZURE"));
        when(stackService.getAllWhereStatusNotIn(anySet())).thenReturn(List.of(jobResource1, jobResource2, jobResource3));

        underTest.initJobs();

        verify(providerSyncJobService, times(3)).schedule((ProviderSyncJobAdapter) any());
    }

    @Test
    void testInitJobsWithoutAliveDatahubs() {
        when(providerSyncConfig.isProviderSyncEnabled()).thenReturn(Boolean.TRUE);
        when(stackService.getAllWhereStatusNotIn(anySet())).thenReturn(List.of());

        underTest.initJobs();

        verify(providerSyncJobService, never()).schedule((ProviderSyncJobAdapter) any());
    }

    @Test
    void testInitJobsWhenProviderSyncIsDisabled() {
        when(providerSyncConfig.isProviderSyncEnabled()).thenReturn(Boolean.FALSE);

        underTest.initJobs();

        verify(stackService, never()).getAllWhereStatusNotIn(anySet());
        verify(providerSyncJobService, never()).schedule((ProviderSyncJobAdapter) any());
    }
}