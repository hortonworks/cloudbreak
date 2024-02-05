package com.sequenceiq.environment.environment.scheduled.archiver;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.cloudbreak.util.TimeUtil;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@ExtendWith(MockitoExtension.class)
class EnvironmentArchiverJobTest {

    @Mock
    private EnvironmentArchiverConfig environmentArchiverConfig;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private CDPStructuredEventDBService structuredEventService;

    @Mock
    private TimeUtil timeUtil;

    @InjectMocks
    private EnvironmentArchiverJob environmentArchiverJob;

    @Test
    public void testExecuteTracedJob() throws JobExecutionException {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(environmentArchiverConfig.getRetentionPeriodInDays()).thenReturn(7);
        when(timeUtil.getTimestampThatDaysBeforeNow(anyInt())).thenReturn(new Date().getTime());
        when(environmentService.getAllForArchive(anyLong())).thenReturn(Set.of("crn1"));
        when(structuredEventService.deleteStructuredEventByResourceCrn("crn1")).thenReturn(Optional.empty());

        environmentArchiverJob.executeTracedJob(context);

        verify(environmentService).deleteByResourceCrn("crn1");
        verify(structuredEventService).deleteStructuredEventByResourceCrn("crn1");
    }
}