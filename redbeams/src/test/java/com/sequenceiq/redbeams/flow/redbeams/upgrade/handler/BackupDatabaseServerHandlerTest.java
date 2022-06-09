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
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.BackupDatabaseServerRequest;
import com.sequenceiq.redbeams.service.stack.DBStackService;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class BackupDatabaseServerHandlerTest {

    @Mock
    private DBStackService dbStackService;

    @InjectMocks
    private BackupDatabaseServerHandler underTest;

    @Test
    void testSelector() {
        assertEquals("BACKUPDATABASESERVERREQUEST", underTest.selector());
    }

    @Test
    void testDoAccept() {
        HandlerEvent<BackupDatabaseServerRequest> event = getHandlerEvent();

        Selectable nextFlowStepSelector = underTest.doAccept(event);

        assertEquals("BACKUPDATABASESERVERSUCCESS", nextFlowStepSelector.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        BackupDatabaseServerRequest backupDatabaseServerRequest = new BackupDatabaseServerRequest(null, null, null, null);

        Selectable defaultFailureEvent = underTest.defaultFailureEvent(1L, new RuntimeException(), Event.wrap(backupDatabaseServerRequest));

        assertEquals("REDBEAMSUPGRADEFAILEDEVENT", defaultFailureEvent.selector());
    }

    private HandlerEvent<BackupDatabaseServerRequest> getHandlerEvent() {
        BackupDatabaseServerRequest backupDatabaseServerRequest = new BackupDatabaseServerRequest(null, null, null, null);
        HandlerEvent<BackupDatabaseServerRequest> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getEvent()).thenReturn(Event.wrap(backupDatabaseServerRequest));
        return handlerEvent;
    }

}
