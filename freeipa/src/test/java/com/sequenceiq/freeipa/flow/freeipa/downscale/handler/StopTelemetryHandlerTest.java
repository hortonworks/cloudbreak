package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stoptelemetry.StopTelemetryRequest;
import com.sequenceiq.freeipa.service.TelemetryAgentService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class StopTelemetryHandlerTest {

    @Mock
    private TelemetryAgentService telemetryAgentService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private StopTelemetryHandler underTest;

    @Test
    void testCallsTelemetryAgentService() {
        List<String> instanceIds = List.of("i-1");
        StopTelemetryRequest request = new StopTelemetryRequest(1L, instanceIds);
        underTest.accept(new Event<>(request));
        verify(telemetryAgentService, times(1)).stopTelemetryAgent(1L, instanceIds);
        verify(eventBus).notify(eq("STOPTELEMETRYRESPONSE"), ArgumentMatchers.<Event>any());
    }
}