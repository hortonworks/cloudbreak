package com.sequenceiq.datalake.flow.diagnostics.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsFlowService;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsFailedEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsSuccessEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsWaitRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class SdxDiagnosticsCollectionHandlerTest {

    @Mock
    private SdxDiagnosticsFlowService diagnosticsFlowService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private final SdxDiagnosticsCollectionHandler underTest = new SdxDiagnosticsCollectionHandler();

    @Test
    void testAcceptHappyPath() {
        Event<SdxDiagnosticsWaitRequest> event = initEvent();

        underTest.accept(event);

        SdxDiagnosticsSuccessEvent sdxDiagnosticsSuccessEvent = new SdxDiagnosticsSuccessEvent(1L, "userId", Map.of());
        Mockito.verify(eventBus, Mockito.times(1)).notify(eq(sdxDiagnosticsSuccessEvent.selector()), any(Event.class));
    }

    @Test
    void testAcceptWithUserBreakException() {
        Event<SdxDiagnosticsWaitRequest> event = initEvent();
        UserBreakException exception = new UserBreakException();
        doThrow(exception).when(diagnosticsFlowService).waitForDiagnosticsCollection(anyLong(), any(), any());

        underTest.accept(event);

        SdxDiagnosticsFailedEvent sdxDiagnosticsFailedEvent = new SdxDiagnosticsFailedEvent(1L, "userId", Map.of(), exception);
        Mockito.verify(eventBus, Mockito.times(1)).notify(eq(sdxDiagnosticsFailedEvent.selector()), any(Event.class));
    }

    @Test
    void testAcceptWithPollerStoppedException() {
        Event<SdxDiagnosticsWaitRequest> event = initEvent();
        PollerStoppedException exception = new PollerStoppedException();
        doThrow(exception).when(diagnosticsFlowService).waitForDiagnosticsCollection(anyLong(), any(), any());

        underTest.accept(event);

        SdxDiagnosticsFailedEvent sdxDiagnosticsFailedEvent = new SdxDiagnosticsFailedEvent(1L, "userId", Map.of(), exception);
        Mockito.verify(eventBus, Mockito.times(1)).notify(eq(sdxDiagnosticsFailedEvent.selector()), any(Event.class));
    }

    @Test
    void testAcceptWithPollerException() {
        Event<SdxDiagnosticsWaitRequest> event = initEvent();
        PollerException exception = new PollerException();
        doThrow(exception).when(diagnosticsFlowService).waitForDiagnosticsCollection(anyLong(), any(), any());

        underTest.accept(event);

        SdxDiagnosticsFailedEvent sdxDiagnosticsFailedEvent = new SdxDiagnosticsFailedEvent(1L, "userId", Map.of(), exception);
        Mockito.verify(eventBus, Mockito.times(1)).notify(eq(sdxDiagnosticsFailedEvent.selector()), any(Event.class));
    }

    @Test
    void testAcceptWithUnspecifiedException() {
        Event<SdxDiagnosticsWaitRequest> event = initEvent();
        RuntimeException exception = new RuntimeException();
        doThrow(exception).when(diagnosticsFlowService).waitForDiagnosticsCollection(anyLong(), any(), any());

        underTest.accept(event);

        SdxDiagnosticsFailedEvent sdxDiagnosticsFailedEvent = new SdxDiagnosticsFailedEvent(1L, "userId", Map.of(), exception);
        Mockito.verify(eventBus, Mockito.times(1)).notify(eq(sdxDiagnosticsFailedEvent.selector()), any(Event.class));
    }

    @NotNull
    private Event<SdxDiagnosticsWaitRequest> initEvent() {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "pollableId");
        SdxDiagnosticsWaitRequest request = new SdxDiagnosticsWaitRequest(1L, "userId", Map.of(), flowIdentifier);
        return new Event<>(new Event.Headers(), request);
    }
}