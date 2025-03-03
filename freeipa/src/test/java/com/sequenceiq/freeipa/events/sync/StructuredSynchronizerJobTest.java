package com.sequenceiq.freeipa.events.sync;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeipaStructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPDefaultStructuredEventClient;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class StructuredSynchronizerJobTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private StructuredSynchronizerJobService structuredSynchronizerJobService;

    @Mock
    private StructuredSyncEventFactory structuredSyncEventFactory;

    @Mock
    private CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private StructuredSynchronizerJob underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "localId", STACK_ID.toString());
    }

    @Test
    void testGetMdcContextConfigProvider() {
        assertEquals(Optional.empty(), underTest.getMdcContextConfigProvider());
    }

    @Test
    void testExecuteTracedJobWhenStackNotFound() {
        when(stackService.getStackById(STACK_ID)).thenThrow(NotFoundException.class);

        underTest.executeTracedJob(jobExecutionContext);

        verify(structuredSynchronizerJobService).unschedule(STACK_ID.toString());
        verifyNoInteractions(jobExecutionContext, structuredSyncEventFactory, cdpDefaultStructuredEventClient);
    }

    @Test
    void testExecuteTracedJobWhenStackIsNull() {
        when(stackService.getStackById(STACK_ID)).thenReturn(null);

        underTest.executeTracedJob(jobExecutionContext);

        verify(structuredSynchronizerJobService).unschedule(STACK_ID.toString());
        verifyNoInteractions(jobExecutionContext, structuredSyncEventFactory, cdpDefaultStructuredEventClient);
    }

    @Test
    void testExecuteTracedJobWhenStackStatusObjectIsNull() {
        Stack stack = mock(Stack.class);
        when(stack.getStackStatus()).thenReturn(null);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);

        underTest.executeTracedJob(jobExecutionContext);

        verifyNoInteractions(jobExecutionContext, structuredSynchronizerJobService, structuredSyncEventFactory, cdpDefaultStructuredEventClient);
    }

    @Test
    void testExecuteTracedJobWhenStackStatusIsNull() {
        Stack stack = mock(Stack.class);
        StackStatus stackStatus = mock(StackStatus.class);
        when(stack.getStackStatus()).thenReturn(stackStatus);
        when(stackStatus.getStatus()).thenReturn(null);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);

        underTest.executeTracedJob(jobExecutionContext);

        verifyNoInteractions(jobExecutionContext, structuredSynchronizerJobService, structuredSyncEventFactory, cdpDefaultStructuredEventClient);
    }

    @Test
    void testExecuteTracedJobWhenStackIsDeleted() {
        Stack stack = mock(Stack.class);
        StackStatus stackStatus = mock(StackStatus.class);
        when(stack.getStackStatus()).thenReturn(stackStatus);
        when(stackStatus.getStatus()).thenReturn(Status.DELETE_COMPLETED);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);

        underTest.executeTracedJob(jobExecutionContext);

        verify(structuredSynchronizerJobService).unschedule(STACK_ID.toString());
        verifyNoInteractions(jobExecutionContext, structuredSyncEventFactory, cdpDefaultStructuredEventClient);
    }

    @Test
    void testExecuteTracedJobWhenStackIsSyncable() {
        Stack stack = mock(Stack.class);
        StackStatus stackStatus = mock(StackStatus.class);
        CDPFreeipaStructuredSyncEvent cdpFreeipaStructuredSyncEvent = mock(CDPFreeipaStructuredSyncEvent.class);
        when(stack.getStackStatus()).thenReturn(stackStatus);
        when(stackStatus.getStatus()).thenReturn(Status.AVAILABLE);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        when(structuredSyncEventFactory.createCDPFreeipaStructuredSyncEvent(STACK_ID)).thenReturn(cdpFreeipaStructuredSyncEvent);

        underTest.executeTracedJob(jobExecutionContext);

        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(cdpFreeipaStructuredSyncEvent);
    }

    @Test
    void testExecuteTracedJobWhenSendingEventThrows() {
        Stack stack = mock(Stack.class);
        StackStatus stackStatus = mock(StackStatus.class);
        CDPFreeipaStructuredSyncEvent cdpFreeipaStructuredSyncEvent = mock(CDPFreeipaStructuredSyncEvent.class);
        when(stack.getStackStatus()).thenReturn(stackStatus);
        when(stackStatus.getStatus()).thenReturn(Status.AVAILABLE);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        when(structuredSyncEventFactory.createCDPFreeipaStructuredSyncEvent(STACK_ID)).thenReturn(cdpFreeipaStructuredSyncEvent);
        doThrow(RuntimeException.class).when(cdpDefaultStructuredEventClient).sendStructuredEvent(cdpFreeipaStructuredSyncEvent);

        assertDoesNotThrow(() -> underTest.executeTracedJob(jobExecutionContext));
    }

}
