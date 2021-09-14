package com.sequenceiq.datalake.flow.datalake.cmsync.handler;

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
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.datalake.cmsync.event.SdxCmSyncFailedEvent;
import com.sequenceiq.datalake.flow.datalake.cmsync.event.SdxCmSyncWaitEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class SdxCmSyncWaitHandlerTest {

    private static final long SDX_ID = 1L;

    private static final String SYNC_CM = "Sync cm";

    private static final int SLEEP_TIME_IN_SEC = 11;

    private static final int DURATION_IN_MINUTES = 12;

    private static final String USER_ID = "userId";

    @Mock
    private SdxWaitService sdxWaitService;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SdxCmSyncWaitHandler underTest;

    @Test
    void testSelector() {
        assertEquals("SDXCMSYNCWAITEVENT", underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        RuntimeException exception = new RuntimeException();
        SdxCmSyncWaitEvent sdxCmSyncWaitEvent = mock(SdxCmSyncWaitEvent.class);
        when(sdxCmSyncWaitEvent.getUserId()).thenReturn(USER_ID);

        Selectable failureSelectable = underTest.defaultFailureEvent(SDX_ID, exception, new Event<>(sdxCmSyncWaitEvent));

        assertEquals("SDXCMSYNCFAILEDEVENT", failureSelectable.selector());
        assertEquals(SDX_ID, failureSelectable.getResourceId());
        assertEquals(exception, ((SdxCmSyncFailedEvent) failureSelectable).getException());
    }

    @Test
    void testAccept() {
        ReflectionTestUtils.setField(underTest, "sleepTimeInSec", SLEEP_TIME_IN_SEC);
        ReflectionTestUtils.setField(underTest, "durationInMinutes", DURATION_IN_MINUTES);
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);

        Selectable nextEvent = underTest.doAccept(getEvent());

        assertEquals("SDX_CM_SYNC_FINISHED_EVENT",  nextEvent.selector());
        assertEquals(SDX_ID, nextEvent.getResourceId());
        verifyPollingConfig(sdxCluster);
    }

    @Test
    void testAcceptWhenSdxWaitServiceThrows() {
        ReflectionTestUtils.setField(underTest, "sleepTimeInSec", SLEEP_TIME_IN_SEC);
        ReflectionTestUtils.setField(underTest, "durationInMinutes", DURATION_IN_MINUTES);
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        doThrow(new SdxWaitException("Polling threw", new RuntimeException())).when(sdxWaitService).waitForCloudbreakFlow(eq(sdxCluster), any(), eq(SYNC_CM));

        Selectable nextEvent = underTest.doAccept(getEvent());

        assertEquals("SDXCMSYNCFAILEDEVENT",  nextEvent.selector());
        assertEquals(SDX_ID, nextEvent.getResourceId());
    }

    private void verifyPollingConfig(SdxCluster sdxCluster) {
        ArgumentCaptor<PollingConfig> pollingConfigArgumentCaptor = ArgumentCaptor.forClass(PollingConfig.class);
        verify(sdxWaitService).waitForCloudbreakFlow(eq(sdxCluster), pollingConfigArgumentCaptor.capture(), eq(SYNC_CM));
        PollingConfig pollingConfig = pollingConfigArgumentCaptor.getValue();
        assertEquals(SLEEP_TIME_IN_SEC, pollingConfig.getSleepTime());
        assertEquals(TimeUnit.SECONDS, pollingConfig.getSleepTimeUnit());
        assertEquals(DURATION_IN_MINUTES, pollingConfig.getDuration());
        assertEquals(TimeUnit.MINUTES, pollingConfig.getDurationTimeUnit());
        assertEquals(Boolean.TRUE, pollingConfig.getStopPollingIfExceptionOccurred());
    }

    private HandlerEvent<SdxCmSyncWaitEvent> getEvent() {
        SdxCmSyncWaitEvent sdxCmSyncWaitEvent = mock(SdxCmSyncWaitEvent.class);
        when(sdxCmSyncWaitEvent.getResourceId()).thenReturn(SDX_ID);
        return new HandlerEvent<>(new Event<>(sdxCmSyncWaitEvent));
    }
}
