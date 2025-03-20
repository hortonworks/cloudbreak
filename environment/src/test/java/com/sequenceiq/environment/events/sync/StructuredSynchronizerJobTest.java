package com.sequenceiq.environment.events.sync;

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
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPDefaultStructuredEventClient;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@ExtendWith(MockitoExtension.class)
class StructuredSynchronizerJobTest {

    private static final Long ENVIRONMENT_ID = 1L;

    @Mock
    private EnvironmentService environmentService;

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
        ReflectionTestUtils.setField(underTest, "localId", ENVIRONMENT_ID.toString());
    }

    @Test
    void testGetMdcContextConfigProvider() {
        assertEquals(Optional.empty(), underTest.getMdcContextConfigProvider());
    }

    @Test
    void testExecuteTracedJobWhenEnvironmentNotFound() {
        when(environmentService.findEnvironmentByIdOrThrow(ENVIRONMENT_ID)).thenThrow(NotFoundException.class);

        underTest.executeJob(jobExecutionContext);

        verify(structuredSynchronizerJobService).unschedule(ENVIRONMENT_ID.toString());
        verifyNoInteractions(jobExecutionContext, structuredSyncEventFactory, cdpDefaultStructuredEventClient);
    }

    @Test
    void testExecuteTracedJobWhenEnvironmentIsNull() {
        when(environmentService.findEnvironmentByIdOrThrow(ENVIRONMENT_ID)).thenReturn(null);

        underTest.executeJob(jobExecutionContext);

        verify(structuredSynchronizerJobService).unschedule(ENVIRONMENT_ID.toString());
        verifyNoInteractions(jobExecutionContext, structuredSyncEventFactory, cdpDefaultStructuredEventClient);
    }

    @Test
    void testExecuteTracedJobWhenEnvironmentStatusIsNull() {
        Environment environment = mock(Environment.class);
        when(environment.getStatus()).thenReturn(null);
        when(environmentService.findEnvironmentByIdOrThrow(ENVIRONMENT_ID)).thenReturn(environment);

        underTest.executeJob(jobExecutionContext);

        verifyNoInteractions(jobExecutionContext, structuredSynchronizerJobService, structuredSyncEventFactory, cdpDefaultStructuredEventClient);
    }

    @Test
    void testExecuteTracedJobWhenEnvironmentIsDeleted() {
        Environment environment = mock(Environment.class);
        when(environment.getStatus()).thenReturn(EnvironmentStatus.ARCHIVED);
        when(environmentService.findEnvironmentByIdOrThrow(ENVIRONMENT_ID)).thenReturn(environment);

        underTest.executeJob(jobExecutionContext);

        verify(structuredSynchronizerJobService).unschedule(ENVIRONMENT_ID.toString());
        verifyNoInteractions(jobExecutionContext, structuredSyncEventFactory, cdpDefaultStructuredEventClient);
    }

    @Test
    void testExecuteTracedJobWhenEnvironmentIsSyncable() {
        Environment environment = mock(Environment.class);
        CDPEnvironmentStructuredSyncEvent cdpEnvironmentStructuredSyncEvent = mock(CDPEnvironmentStructuredSyncEvent.class);
        when(environment.getStatus()).thenReturn(EnvironmentStatus.AVAILABLE);
        when(environmentService.findEnvironmentByIdOrThrow(ENVIRONMENT_ID)).thenReturn(environment);
        when(structuredSyncEventFactory.createCDPEnvironmentStructuredSyncEvent(ENVIRONMENT_ID)).thenReturn(cdpEnvironmentStructuredSyncEvent);

        underTest.executeJob(jobExecutionContext);

        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(cdpEnvironmentStructuredSyncEvent);
    }

    @Test
    void testExecuteTracedJobWhenSendingEventThrows() {
        Environment environment = mock(Environment.class);
        CDPEnvironmentStructuredSyncEvent cdpEnvironmentStructuredSyncEvent = mock(CDPEnvironmentStructuredSyncEvent.class);
        when(environment.getStatus()).thenReturn(EnvironmentStatus.AVAILABLE);
        when(environmentService.findEnvironmentByIdOrThrow(ENVIRONMENT_ID)).thenReturn(environment);
        when(structuredSyncEventFactory.createCDPEnvironmentStructuredSyncEvent(ENVIRONMENT_ID)).thenReturn(cdpEnvironmentStructuredSyncEvent);
        doThrow(RuntimeException.class).when(cdpDefaultStructuredEventClient).sendStructuredEvent(cdpEnvironmentStructuredSyncEvent);

        assertDoesNotThrow(() -> underTest.executeJob(jobExecutionContext));
    }
}
