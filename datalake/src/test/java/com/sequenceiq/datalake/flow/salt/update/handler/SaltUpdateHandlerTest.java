package com.sequenceiq.datalake.flow.salt.update.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateFailureResponse;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateRequest;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateSuccessResponse;
import com.sequenceiq.datalake.service.sdx.CloudbreakStackService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class SaltUpdateHandlerTest {

    private static final String USER_ID = "user-id";

    private static final long SDX_ID = 1L;

    private static final String SALT_UPDATE = "Running Salt update";

    private static final int SLEEP_TIME_IN_SEC = 10;

    private static final int DURATION_IN_MINUTES = 120;

    @Mock
    private CloudbreakStackService cloudbreakStackService;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SaltUpdateHandler underTest;

    @Test
    void testSelector() {
        assertEquals("SALTUPDATEREQUEST", underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable failureEvent = underTest.defaultFailureEvent(SDX_ID, new Exception("error"), new Event<>(new SaltUpdateRequest(SDX_ID, USER_ID)));

        assertEquals("SALTUPDATEFAILURERESPONSE", failureEvent.getSelector());
        assertEquals(SDX_ID, failureEvent.getResourceId());
        assertEquals("error", ((SaltUpdateFailureResponse) failureEvent).getException().getMessage());
    }

    @Test
    void testAccept() {
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);

        Selectable nextEvent = underTest.doAccept(getEvent());

        assertEquals("SALTUPDATESUCCESSRESPONSE", nextEvent.selector());
        assertEquals(SDX_ID, nextEvent.getResourceId());
        assertEquals(USER_ID, ((SaltUpdateSuccessResponse) nextEvent).getUserId());
        verify(cloudbreakStackService, times(1)).updateSaltByName(sdxCluster, false);
    }

    @Test
    void testAcceptWhenCloudbreakStackServiceThrows() {
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        doThrow(new RuntimeException("error")).when(cloudbreakStackService).updateSaltByName(eq(sdxCluster), eq(false));

        Selectable nextEvent = underTest.doAccept(getEvent());

        assertEquals("SALTUPDATEFAILURERESPONSE", nextEvent.selector());
        assertEquals(SDX_ID, nextEvent.getResourceId());
        assertEquals(USER_ID, ((SaltUpdateFailureResponse) nextEvent).getUserId());
        assertEquals("error", ((SaltUpdateFailureResponse) nextEvent).getException().getMessage());
        verify(cloudbreakStackService, times(1)).updateSaltByName(sdxCluster, false);
    }

    private HandlerEvent<SaltUpdateRequest> getEvent() {
        SaltUpdateRequest saltUpdateRequest = mock(SaltUpdateRequest.class);
        when(saltUpdateRequest.getResourceId()).thenReturn(SDX_ID);
        when(saltUpdateRequest.getUserId()).thenReturn(USER_ID);
        return new HandlerEvent<>(new Event<>(saltUpdateRequest));
    }
}
