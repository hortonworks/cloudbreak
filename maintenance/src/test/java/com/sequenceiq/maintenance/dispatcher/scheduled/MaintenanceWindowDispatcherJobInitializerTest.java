package com.sequenceiq.maintenance.dispatcher.scheduled;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowDispatcherJobInitializerTest {

    @Mock
    private MaintenanceWindowDispatcherJobService dispatcherJobService;

    @InjectMocks
    private MaintenanceWindowDispatcherJobInitializer underTest;

    @Test
    void initJobsSchedulesDispatcherJob() {
        underTest.initJobs();

        verify(dispatcherJobService).schedule();
    }
}
