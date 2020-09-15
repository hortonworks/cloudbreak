package com.sequenceiq.datalake.flow.stop.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.datalake.flow.stop.event.SdxStopAllDatahubRequest;
import com.sequenceiq.datalake.flow.stop.event.SdxStopFailedEvent;
import com.sequenceiq.datalake.service.sdx.stop.SdxStopService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
public class SdxStopAllDatahubHandlerTest {

    private static final long SDX_ID = 1L;

    private static final String USER_ID = "USER_ID";

    @Captor
    private ArgumentCaptor<Event<SdxStopFailedEvent>> captor;

    @Mock
    private EventBus eventBus;

    @Mock
    private SdxStopService sdxStopService;

    @InjectMocks
    private SdxStopAllDatahubHandler underTest;

    @Test
    public void testUserBreakException() {
        UserBreakException userBreakException = new UserBreakException("");
        doThrow(userBreakException).when(sdxStopService).stopAllDatahub(anyLong());
        underTest.accept(new Event<>(new SdxStopAllDatahubRequest(1L, "USER_ID")));

        verify(eventBus).notify(anyString(), captor.capture());

        SdxStopFailedEvent sdxStopFailedEvent = captor.getValue().getData();

        assertEquals(SDX_ID, sdxStopFailedEvent.getResourceId());
        assertEquals(USER_ID, sdxStopFailedEvent.getUserId());
        assertEquals(userBreakException, sdxStopFailedEvent.getException());
    }

    @Test
    public void testBadRequestException() {
        BadRequestException badRequestException = new BadRequestException("");
        doThrow(badRequestException).when(sdxStopService).stopAllDatahub(anyLong());
        underTest.accept(new Event<>(new SdxStopAllDatahubRequest(1L, "USER_ID")));

        verify(eventBus).notify(anyString(), captor.capture());

        SdxStopFailedEvent sdxStopFailedEvent = captor.getValue().getData();

        assertEquals(SDX_ID, sdxStopFailedEvent.getResourceId());
        assertEquals(USER_ID, sdxStopFailedEvent.getUserId());
        assertEquals(badRequestException, sdxStopFailedEvent.getException());
    }

    @Test
    public void testPollerException() {
        PollerException pollerException = new PollerException("");
        doThrow(pollerException).when(sdxStopService).stopAllDatahub(anyLong());
        underTest.accept(new Event<>(new SdxStopAllDatahubRequest(SDX_ID, USER_ID)));

        verify(eventBus).notify(anyString(), captor.capture());

        SdxStopFailedEvent sdxStopFailedEvent = captor.getValue().getData();

        assertEquals(SDX_ID, sdxStopFailedEvent.getResourceId());
        assertEquals(USER_ID, sdxStopFailedEvent.getUserId());
        assertEquals(pollerException, sdxStopFailedEvent.getException());
    }

    @Test
    public void testPollerStoppedException() {
        PollerStoppedException pollerStoppedException = new PollerStoppedException("");
        doThrow(pollerStoppedException).when(sdxStopService).stopAllDatahub(anyLong());
        underTest.accept(new Event<>(new SdxStopAllDatahubRequest(SDX_ID, USER_ID)));

        verify(eventBus).notify(anyString(), captor.capture());

        SdxStopFailedEvent sdxStopFailedEvent = captor.getValue().getData();

        assertEquals(SDX_ID, sdxStopFailedEvent.getResourceId());
        assertEquals(USER_ID, sdxStopFailedEvent.getUserId());
        assertEquals("Datalake stop timed out after 40 minutes",
                sdxStopFailedEvent.getException().getMessage());
    }
}