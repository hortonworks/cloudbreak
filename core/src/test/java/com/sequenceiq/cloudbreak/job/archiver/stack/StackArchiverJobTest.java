package com.sequenceiq.cloudbreak.job.archiver.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.db.LegacyStructuredEventDBService;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.cloudbreak.util.TimeUtil;

@ExtendWith(MockitoExtension.class)
class StackArchiverJobTest {

    @InjectMocks
    private StackArchiverJob stackArchiverJob;

    @Mock
    private StackArchiverConfig stackArchiverConfig;

    @Mock
    private StackService stackService;

    @Mock
    private CDPStructuredEventDBService structuredEventService;

    @Mock
    private LegacyStructuredEventDBService legacyStructuredEventDBService;

    @Mock
    private TimeUtil timeUtil;

    @Mock
    private StackArchiverJobService stackArchiverJobService;

    @Test
    public void testExecuteTracedJob() throws Exception {
        when(stackArchiverConfig.getRetentionPeriodInDays()).thenReturn(30);
        when(stackArchiverConfig.getLimitForStack()).thenReturn(500);
        when(timeUtil.getTimestampThatDaysBeforeNow(30)).thenReturn(1646259968L);
        stackArchiverJob.executeTracedJob(null);
        verify(stackService, times(1)).getAllForArchive(1646259968L, 500);
        verify(stackArchiverJobService, times(1)).reschedule();
    }

    @Test
    public void testExecuteTracedJobWhenPurgeFailed() throws Exception {
        when(stackArchiverConfig.getRetentionPeriodInDays()).thenReturn(30);
        when(stackArchiverConfig.getLimitForStack()).thenReturn(500);
        when(timeUtil.getTimestampThatDaysBeforeNow(30)).thenReturn(1646259968L);
        when(stackService.getAllForArchive(anyLong(), anyInt())).thenReturn(Set.of("crn1", "crn2"));
        lenient().doThrow(new RuntimeException("failed")).when(structuredEventService).deleteStructuredEventByResourceCrn(eq("crn1"));
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> stackArchiverJob.executeTracedJob(null));
        assertEquals("Failed to purge finalzied stacks: {crn1=failed}", runtimeException.getMessage());
        verify(stackService, times(1)).getAllForArchive(1646259968L, 500);
        verify(structuredEventService, times(1)).deleteStructuredEventByResourceCrn(eq("crn1"));
        verify(legacyStructuredEventDBService, never()).deleteEntriesByResourceCrn(eq("crn1"));
        verify(stackService, never()).deleteArchivedByResourceCrn(eq("crn1"));
        verify(structuredEventService, times(1)).deleteStructuredEventByResourceCrn(eq("crn2"));
        verify(legacyStructuredEventDBService, times(1)).deleteEntriesByResourceCrn(eq("crn2"));
        verify(stackService, times(1)).deleteArchivedByResourceCrn(eq("crn2"));
        verify(stackArchiverJobService, times(1)).reschedule();
    }

    @Test
    public void testPurgeFinalizedStacks() throws Exception {
        when(stackService.getAllForArchive(anyLong(), anyInt())).thenReturn(Set.of("crn1", "crn2"));

        stackArchiverJob.purgeFinalizedStacks(30);

        verify(structuredEventService, times(2)).deleteStructuredEventByResourceCrn(anyString());
        verify(legacyStructuredEventDBService, times(2)).deleteEntriesByResourceCrn(anyString());
        verify(stackService, times(2)).deleteArchivedByResourceCrn(anyString());
    }
}