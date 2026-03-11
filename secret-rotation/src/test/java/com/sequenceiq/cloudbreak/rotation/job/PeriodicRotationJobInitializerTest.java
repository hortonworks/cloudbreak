package com.sequenceiq.cloudbreak.rotation.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties;
import com.sequenceiq.cloudbreak.rotation.service.periodic.PeriodicSecretRotationService;

@ExtendWith(MockitoExtension.class)
class PeriodicRotationJobInitializerTest {

    @Mock
    private PeriodicRotationJobService scheduler;

    @Mock
    private PeriodicSecretRotationService rotationService;

    @Mock
    private PeriodicRotationProperties periodicRotationProperties;

    @InjectMocks
    private PeriodicRotationJobInitializer underTest;

    @Test
    void initJobsSchedulesWhenEnabled() {
        JobResource jobResource1 = mock(JobResource.class);
        JobResource jobResource2 = mock(JobResource.class);
        when(jobResource1.getRemoteResourceId()).thenReturn("crn:1");
        when(jobResource2.getRemoteResourceId()).thenReturn("crn:2");
        when(periodicRotationProperties.isEnabled()).thenReturn(true);
        when(rotationService.listJobResources()).thenReturn(List.of(jobResource1, jobResource2));

        underTest.initJobs();

        ArgumentCaptor<PeriodicRotationJobAdapter> adapterCaptor = ArgumentCaptor.forClass(PeriodicRotationJobAdapter.class);
        verify(scheduler, times(2)).schedule(adapterCaptor.capture());
        List<PeriodicRotationJobAdapter> adapters = adapterCaptor.getAllValues();
        assertThat(adapters).extracting(a -> a.getJobResource().getRemoteResourceId())
                .containsExactlyInAnyOrder("crn:1", "crn:2");
    }

    @Test
    void initJobsSkipsWhenDisabled() {
        when(periodicRotationProperties.isEnabled()).thenReturn(false);

        underTest.initJobs();

        verify(scheduler, never()).schedule(any(PeriodicRotationJobAdapter.class));
    }
}

