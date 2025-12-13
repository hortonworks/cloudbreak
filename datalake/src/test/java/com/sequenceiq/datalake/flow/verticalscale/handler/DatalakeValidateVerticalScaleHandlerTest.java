package com.sequenceiq.datalake.flow.verticalscale.handler;

import static com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleHandlerSelectors.VERTICAL_SCALING_DATALAKE_VALIDATION_HANDLER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleEvent;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleFailedEvent;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleStateSelectors;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
public class DatalakeValidateVerticalScaleHandlerTest {

    @Mock
    private EventSender eventSender;

    private DatalakeValidateVerticalScaleHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new DatalakeValidateVerticalScaleHandler(eventSender);
    }

    @Test
    public void testValidateVerticalScaleActionSuccessEvent() {
        ArgumentCaptor<DatalakeVerticalScaleEvent> eventCaptor = ArgumentCaptor.forClass(DatalakeVerticalScaleEvent.class);
        DatalakeVerticalScaleEvent datalakeVerticalScaleEvent = getDatalakeVerticalScaleEvent();
        Event event = new Event<>(datalakeVerticalScaleEvent);
        underTest.accept(event);
        verify(eventSender).sendEvent(eventCaptor.capture(), eq(event.getHeaders()));
        assertEquals(DatalakeVerticalScaleStateSelectors.VERTICAL_SCALING_DATALAKE_EVENT.selector(), eventCaptor.getValue().getSelector());
    }

    @Test
    public void testValidateVerticalScaleActionFailureEvent() {
        ArgumentCaptor<DatalakeVerticalScaleFailedEvent> eventCaptor = ArgumentCaptor.forClass(DatalakeVerticalScaleFailedEvent.class);
        DatalakeVerticalScaleEvent datalakeVerticalScaleEvent = mock(DatalakeVerticalScaleEvent.class);
        doThrow(new NullPointerException("Test")).when(datalakeVerticalScaleEvent).getVerticalScaleRequest();
        Event event = new Event<>(datalakeVerticalScaleEvent);
        underTest.accept(event);
        verify(eventSender).sendEvent(eventCaptor.capture(), eq(event.getHeaders()));
        assertEquals(DatalakeVerticalScaleStateSelectors.FAILED_VERTICAL_SCALING_DATALAKE_EVENT.selector(), eventCaptor.getValue().getSelector());
        assertEquals(DatalakeStatusEnum.DATALAKE_VERTICAL_SCALE_FAILED, eventCaptor.getValue().getDatalakeStatus());
        assertEquals("Test", eventCaptor.getValue().getException().getMessage());
    }

    private DatalakeVerticalScaleEvent getDatalakeVerticalScaleEvent() {
        return new DatalakeVerticalScaleEvent(
                VERTICAL_SCALING_DATALAKE_VALIDATION_HANDLER.selector(),
                1L,
                new Promise<>(),
                "Test",
                "test-crn",
                "stack-crn",
                new StackVerticalScaleV4Request()
        );
    }
}
