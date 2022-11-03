package com.sequenceiq.redbeams.flow.redbeams.upgrade.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RestoreDatabaseServerRequest;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
public class RestoreDatabaseServerHandlerTest {
    @Mock
    private DBStackService dbStackService;

    @InjectMocks
    private RestoreDatabaseServerHandler underTest;

    @Test
    void testSelector() {
        assertEquals("RESTOREDATABASESERVERREQUEST", underTest.selector());
    }

    @Test
    void testDoAccept() {
        HandlerEvent<RestoreDatabaseServerRequest> event = getHandlerEvent();

        Selectable nextFlowStepSelector = underTest.doAccept(event);

        assertEquals("RESTOREDATABASESERVERSUCCESS", nextFlowStepSelector.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        RestoreDatabaseServerRequest restoreDatabaseServerRequest = new RestoreDatabaseServerRequest(null, null, null, null);

        Selectable defaultFailureEvent = underTest.defaultFailureEvent(1L, new RuntimeException(), Event.wrap(restoreDatabaseServerRequest));

        assertEquals("REDBEAMSUPGRADEFAILEDEVENT", defaultFailureEvent.selector());
    }

    private HandlerEvent<RestoreDatabaseServerRequest> getHandlerEvent() {
        RestoreDatabaseServerRequest restoreDatabaseServerRequest = new RestoreDatabaseServerRequest(null, null, null, null);
        HandlerEvent<RestoreDatabaseServerRequest> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getEvent()).thenReturn(Event.wrap(restoreDatabaseServerRequest));
        return handlerEvent;
    }

}
