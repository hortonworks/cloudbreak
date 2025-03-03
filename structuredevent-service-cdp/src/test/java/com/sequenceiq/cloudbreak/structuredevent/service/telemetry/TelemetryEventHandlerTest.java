package com.sequenceiq.cloudbreak.structuredevent.service.telemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeipaStructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.CDPTelemetryFlowEventLogger;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.freeipa.CDPFreeIpaSyncLogger;

@ExtendWith(MockitoExtension.class)
class TelemetryEventHandlerTest {

    @InjectMocks
    private TelemetryEventHandler underTest;

    @Spy
    private List<CDPTelemetryFlowEventLogger> flowEventLoggers = new ArrayList<>();

    @Mock
    private CDPTelemetryFlowEventLogger flowEventLogger;

    @Mock
    private CDPFreeIpaSyncLogger cdpFreeIpaSyncLogger;

    @BeforeEach()
    void setUp() {
        flowEventLoggers.add(flowEventLogger);
    }

    @Test
    void testAcceptWhenDataIsNull() {
        underTest.accept(new Event(null));

        verifyNoInteractions(flowEventLogger);
    }

    @Test
    void testAcceptWhenNoAcceptableEventFound() {
        when(flowEventLogger.acceptableEventClass()).thenReturn(CDPEnvironmentStructuredFlowEvent.class);
        CDPStructuredFlowEvent cdpStructuredFlowEvent = new CDPStructuredFlowEvent();

        underTest.accept(new Event(cdpStructuredFlowEvent));

        verify(flowEventLogger, times(1)).acceptableEventClass();
        verify(flowEventLogger, never()).log(any());
    }

    @Test
    void testAcceptWhenAcceptableEventFound() {
        when(flowEventLogger.acceptableEventClass()).thenReturn(CDPEnvironmentStructuredFlowEvent.class);
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();

        underTest.accept(new Event(cdpStructuredFlowEvent));

        verify(flowEventLogger, times(1)).acceptableEventClass();
        verify(flowEventLogger, times(1)).log(cdpStructuredFlowEvent);
    }

    @Test
    void testAcceptWhenFreeIpaSyncEvent() {
        CDPFreeipaStructuredSyncEvent cdpFreeipaStructuredSyncEvent = new CDPFreeipaStructuredSyncEvent();

        underTest.accept(new Event(cdpFreeipaStructuredSyncEvent));

        verify(cdpFreeIpaSyncLogger).log(cdpFreeipaStructuredSyncEvent);
        verifyNoInteractions(flowEventLogger);
    }

}