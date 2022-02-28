package com.sequenceiq.redbeams.sync;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
public class DBStackJobInizializerTest {

    @Mock
    private DBStackService dbStackService;

    @Mock
    private DBStackJobService dbStackJobService;

    @InjectMocks
    private DBStackJobInizializer victim;

    @Test
    public void shouldDeleteAllJobsAndScheduleNewOnesForDbStacks() {
        JobResource jobResource = mock(JobResource.class);
        Set<JobResource> jobResources = Set.of(jobResource);

        when(dbStackService.findAllForAutoSync()).thenReturn(jobResources);

        victim.initJobs();

        verify(dbStackJobService).schedule(jobResource);
    }
}