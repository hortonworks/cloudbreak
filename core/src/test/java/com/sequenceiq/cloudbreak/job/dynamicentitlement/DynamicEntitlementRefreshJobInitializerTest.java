package com.sequenceiq.cloudbreak.job.dynamicentitlement;

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
class DynamicEntitlementRefreshJobInitializerTest {
    @Mock
    private StackService stackService;

    @Mock
    private DynamicEntitlementRefreshJobService dynamicEntitlementRefreshJobService;

    @Mock
    private DynamicEntitlementRefreshConfig dynamicEntitlementRefreshConfig;

    @InjectMocks
    private DynamicEntitlementRefreshJobInitializer underTest;

    @Test
    void testInitJobsWithAliveDatahubs() {
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        JobResource jobResource1 = mock(JobResource.class);
        JobResource jobResource2 = mock(JobResource.class);
        when(stackService.getAllWhereStatusNotIn(anySet())).thenReturn(List.of(jobResource1, jobResource2));
        underTest.initJobs();
        verify(dynamicEntitlementRefreshJobService, times(2)).schedule((DynamicEntitlementRefreshJobAdapter) any());
    }

    @Test
    void testInitJobsWithoutAliveDatahubs() {
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        when(stackService.getAllWhereStatusNotIn(anySet())).thenReturn(List.of());
        underTest.initJobs();
        verify(dynamicEntitlementRefreshJobService, never()).schedule((DynamicEntitlementRefreshJobAdapter) any());
    }

}