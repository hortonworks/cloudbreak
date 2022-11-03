package com.sequenceiq.datalake.flow.salt.update.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateFailureResponse;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateWaitRequest;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateWaitSuccessResponse;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class SaltUpdateWaitHandlerTest {

    private static final String USER_ID = "user-id";

    private static final long SDX_ID = 1L;

    private static final String SALT_UPDATE = "Running Salt update";

    private static final int SLEEP_TIME_IN_SEC = 10;

    private static final int DURATION_IN_MINUTES = 120;

    @Mock
    private SdxWaitService sdxWaitService;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SaltUpdateWaitHandler underTest;

    @Test
    void testSelector() {
        assertEquals("SALTUPDATEWAITREQUEST", underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable failureEvent = underTest.defaultFailureEvent(SDX_ID, new Exception("error"), new Event<>(new SaltUpdateWaitRequest(SDX_ID, USER_ID)));

        assertEquals("SALTUPDATEFAILURERESPONSE", failureEvent.getSelector());
        assertEquals(SDX_ID, failureEvent.getResourceId());
        assertEquals("error", ((SaltUpdateFailureResponse) failureEvent).getException().getMessage());
    }

    @Test
    void testAccept() {
        ReflectionTestUtils.setField(underTest, "sleepTimeInSec", SLEEP_TIME_IN_SEC);
        ReflectionTestUtils.setField(underTest, "durationInMinutes", DURATION_IN_MINUTES);
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);

        Selectable nextEvent = underTest.doAccept(getEvent());

        assertEquals("SALTUPDATEWAITSUCCESSRESPONSE",  nextEvent.selector());
        assertEquals(SDX_ID, nextEvent.getResourceId());
        assertEquals(USER_ID, ((SaltUpdateWaitSuccessResponse) nextEvent).getUserId());
        verifyPollingConfig(sdxCluster);
    }

    @Test
    void testAcceptWhenSdxWaitServiceThrows() {
        ReflectionTestUtils.setField(underTest, "sleepTimeInSec", SLEEP_TIME_IN_SEC);
        ReflectionTestUtils.setField(underTest, "durationInMinutes", DURATION_IN_MINUTES);
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        doThrow(new SdxWaitException("Polling threw", new RuntimeException("error")))
                .when(sdxWaitService).waitForCloudbreakFlow(eq(sdxCluster), any(), eq(SALT_UPDATE));

        Selectable nextEvent = underTest.doAccept(getEvent());

        assertEquals("SALTUPDATEFAILURERESPONSE",  nextEvent.selector());
        assertEquals(SDX_ID, nextEvent.getResourceId());
        assertEquals(USER_ID, ((SaltUpdateFailureResponse) nextEvent).getUserId());
        assertEquals("Polling threw", ((SaltUpdateFailureResponse) nextEvent).getException().getMessage());
    }

    private void verifyPollingConfig(SdxCluster sdxCluster) {
        ArgumentCaptor<PollingConfig> pollingConfigArgumentCaptor = ArgumentCaptor.forClass(PollingConfig.class);
        verify(sdxWaitService).waitForCloudbreakFlow(eq(sdxCluster), pollingConfigArgumentCaptor.capture(), eq(SALT_UPDATE));
        PollingConfig pollingConfig = pollingConfigArgumentCaptor.getValue();
        assertEquals(SLEEP_TIME_IN_SEC, pollingConfig.getSleepTime());
        assertEquals(TimeUnit.SECONDS, pollingConfig.getSleepTimeUnit());
        assertEquals(DURATION_IN_MINUTES, pollingConfig.getDuration());
        assertEquals(TimeUnit.MINUTES, pollingConfig.getDurationTimeUnit());
        assertEquals(Boolean.TRUE, pollingConfig.getStopPollingIfExceptionOccurred());
    }

    private HandlerEvent<SaltUpdateWaitRequest> getEvent() {
        SaltUpdateWaitRequest saltUpdateWaitRequest = mock(SaltUpdateWaitRequest.class);
        when(saltUpdateWaitRequest.getResourceId()).thenReturn(SDX_ID);
        when(saltUpdateWaitRequest.getUserId()).thenReturn(USER_ID);
        return new HandlerEvent<>(new Event<>(saltUpdateWaitRequest));
    }
}