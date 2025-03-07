package com.sequenceiq.environment.events.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@ExtendWith(MockitoExtension.class)
class StructuredSynchronizerJobInitializerTest {

    @Mock
    private StructuredSynchronizerConfig structuredSynchronizerConfig;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private StructuredSynchronizerJobService jobService;

    @InjectMocks
    private StructuredSynchronizerJobInitializer underTest;

    @Captor
    private ArgumentCaptor<StructuredSynchronizerJobAdapter> jobAdapterCaptor;

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void testInitJobs(boolean structuredSyncEnabled) {
        JobResource jobResource1 = mock(JobResource.class);
        JobResource jobResource2 = mock(JobResource.class);
        when(structuredSynchronizerConfig.isStructuredSyncEnabled()).thenReturn(structuredSyncEnabled);
        lenient().when(environmentService.findAllAliveForAutoSync(EnumSet.of(EnvironmentStatus.ARCHIVED))).thenReturn(List.of(jobResource1, jobResource2));

        underTest.initJobs();

        if (structuredSyncEnabled) {
            verify(jobService, times(2)).schedule(jobAdapterCaptor.capture(), eq(true));
            List<StructuredSynchronizerJobAdapter> capturedAdapters = jobAdapterCaptor.getAllValues();
            assertEquals(jobResource1, capturedAdapters.get(0).getJobResource());
            assertEquals(jobResource2, capturedAdapters.get(1).getJobResource());
        } else {
            verifyNoInteractions(environmentService, jobService);
        }
    }
}
