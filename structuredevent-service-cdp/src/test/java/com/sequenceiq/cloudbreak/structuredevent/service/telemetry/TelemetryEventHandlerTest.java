package com.sequenceiq.cloudbreak.structuredevent.service.telemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.CDPTelemetryEventLogger;

@ExtendWith(MockitoExtension.class)
class TelemetryEventHandlerTest {

    @InjectMocks
    private TelemetryEventHandler underTest;

    @Spy
    private List<CDPTelemetryEventLogger> eventLoggers = new ArrayList<>();

    @Mock
    private CDPTelemetryEventLogger eventLogger;

    @BeforeEach()
    void setUp() {
        eventLoggers.add(eventLogger);
    }

    @Test
    void testAcceptWhenDataIsNull() {
        underTest.accept(new Event(null));
        Mockito.verify(eventLogger, never()).acceptableEventClass();
        Mockito.verify(eventLogger, never()).log(any());
    }

    @Test
    void testAcceptWhenNoAcceptableEventFound() {
        when(eventLogger.acceptableEventClass()).thenReturn(CDPEnvironmentStructuredFlowEvent.class);
        CDPStructuredFlowEvent cdpStructuredFlowEvent = new CDPStructuredFlowEvent();
        underTest.accept(new Event(cdpStructuredFlowEvent));
        Mockito.verify(eventLogger, times(1)).acceptableEventClass();
        Mockito.verify(eventLogger, never()).log(any());
    }

    @Test
    void testAcceptWhenAcceptableEventFound() {
        when(eventLogger.acceptableEventClass()).thenReturn(CDPEnvironmentStructuredFlowEvent.class);
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        underTest.accept(new Event(cdpStructuredFlowEvent));
        Mockito.verify(eventLogger, times(1)).acceptableEventClass();
        Mockito.verify(eventLogger, times(1)).log(eq(cdpStructuredFlowEvent));
    }

}