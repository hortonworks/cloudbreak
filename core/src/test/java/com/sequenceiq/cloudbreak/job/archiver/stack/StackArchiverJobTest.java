package com.sequenceiq.cloudbreak.job.archiver.stack;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExecuteTracedJob() throws Exception {
        when(stackArchiverConfig.getRetentionPeriodInDays()).thenReturn(30);
        when(timeUtil.getTimestampThatDaysBeforeNow(30)).thenReturn(1646259968L);
        stackArchiverJob.executeTracedJob(null);
        verify(stackService, times(1)).getAllForArchive(1646259968L);
    }

    @Test
    public void testPurgeFinalisedStacks() throws Exception {
        when(stackService.getAllForArchive(anyLong())).thenReturn(Set.of("crn1", "crn2"));
        when(structuredEventService.deleteStructuredEventByResourceCrn(anyString())).thenReturn(Optional.empty());

        stackArchiverJob.purgeFinalisedStacks(30);

        verify(structuredEventService, times(2)).deleteStructuredEventByResourceCrn(anyString());
        verify(legacyStructuredEventDBService, times(2)).deleteEntriesByResourceCrn(anyString());
        verify(stackService, times(2)).deleteArchivedByResourceCrn(anyString());
    }
}