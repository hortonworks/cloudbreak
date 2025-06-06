package com.sequenceiq.cloudbreak.structuredevent.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyDefaultStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.StructuredSyncEventFactory;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

@ExtendWith(MockitoExtension.class)
public class StructuredSynchronizerJobTest {

    @Mock
    private StackService stackService;

    @Mock
    private StructuredSynchronizerJobService syncJobService;

    @Mock
    private StructuredSyncEventFactory structuredSyncEventFactory;

    @Mock
    private LegacyDefaultStructuredEventClient legacyDefaultStructuredEventClient;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private StructuredSynchronizerJob underTest;

    private Stack stack;

    @BeforeEach
    public void init() {
        underTest = new StructuredSynchronizerJob();
        MockitoAnnotations.initMocks(this);
        underTest.setLocalId("1");

        stack = new Stack();
        stack.setId(1L);
    }

    @Test
    public void testUnscheduleJobWhenStackServiceThrowsNotFoundException() throws JobExecutionException {
        when(stackService.get(anyLong())).thenThrow(new NotFoundException("Stack not found"));
        underTest.executeJob(jobExecutionContext);

        verify(syncJobService, times(1)).unschedule("1");
    }

    @Test
    public void testUnscheduleJobWhenStackServiceReturnNull() throws JobExecutionException {
        when(stackService.get(anyLong())).thenReturn(null);
        underTest.executeJob(jobExecutionContext);

        verify(syncJobService, times(1)).unschedule("1");
    }

    @Test
    public void testUnscheduleJobWhenStackIsInUnschedulableState() throws JobExecutionException {
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.DELETE_COMPLETED));
        when(stackService.get(anyLong())).thenReturn(stack);
        underTest.executeJob(jobExecutionContext);

        verify(syncJobService, times(1)).unschedule("1");
    }

    @Test
    public void testNoActionWhenStackStateIsNull() throws JobExecutionException {
        stack.setStackStatus(null);
        when(stackService.get(anyLong())).thenReturn(stack);
        underTest.executeJob(jobExecutionContext);

        verify(syncJobService, times(0)).unschedule(any());
        verify(structuredSyncEventFactory, times(0)).createStructuredSyncEvent(any());
        verify(legacyDefaultStructuredEventClient, times(0)).sendStructuredEvent(any());
    }

    @Test
    public void testJobRunsWhenStackIsInCorrectState() throws JobExecutionException {
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        when(stackService.get(anyLong())).thenReturn(stack);
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        when(structuredSyncEventFactory.createStructuredSyncEvent(1L)).thenReturn(structuredSyncEvent);
        underTest.executeJob(jobExecutionContext);

        verify(syncJobService, times(0)).unschedule(any());
        verify(structuredSyncEventFactory, times(1)).createStructuredSyncEvent(1L);
        verify(legacyDefaultStructuredEventClient, times(1)).sendStructuredEvent(structuredSyncEvent);
    }
}