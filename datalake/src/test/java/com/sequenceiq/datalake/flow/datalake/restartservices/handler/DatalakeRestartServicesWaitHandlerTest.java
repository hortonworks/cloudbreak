package com.sequenceiq.datalake.flow.datalake.restartservices.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowEvent;
import com.sequenceiq.datalake.flow.datalake.restartservices.event.DatalakeRestartServicesFailedEvent;
import com.sequenceiq.datalake.flow.datalake.restartservices.event.DatalakeRestartServicesWaitEvent;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DatalakeRestartServicesWaitHandlerTest {

    private static final Long SDX_ID = 1L;

    private static final String USER_ID = "userId";

    private static final int SLEEP_TIME_IN_SEC = 11;

    private static final int DURATION_IN_MINUTES = 12;

    @Mock
    private SdxService sdxService;

    @Mock
    private CloudbreakPoller poller;

    @InjectMocks
    private DatalakeRestartServicesWaitHandler underTest;

    @Mock
    private SdxCluster sdxCluster;

    @Mock
    private HandlerEvent<DatalakeRestartServicesWaitEvent> event;

    private DatalakeRestartServicesWaitEvent sdxEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "sleepTimeInSec", SLEEP_TIME_IN_SEC);
        ReflectionTestUtils.setField(underTest, "durationInMinutes", DURATION_IN_MINUTES);
        sdxEvent = new DatalakeRestartServicesWaitEvent(SDX_ID, USER_ID);
        when(event.getData()).thenReturn(sdxEvent);
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
    }

    @Test
    void doAcceptSuccess() {
        SdxEvent result = (SdxEvent) underTest.doAccept(event);

        assertThat(result)
                .returns(SDX_ID, SdxEvent::getResourceId)
                .returns(USER_ID, SdxEvent::getUserId)
                .returns(DatalakeRestartServicesFlowEvent.DATALAKE_RESTART_SERVICES_FINISHED_EVENT.event(), SdxEvent::selector);
        verifyPollingConfig();
    }

    @Test
    void doAcceptUserBreakException() {
        UserBreakException exception = new UserBreakException("exception");
        doThrow(exception).when(poller).pollServiceRestartUntilAvailable(any(), any());

        DatalakeRestartServicesFailedEvent result = (DatalakeRestartServicesFailedEvent) underTest.doAccept(event);

        assertThat(result)
                .returns(SDX_ID, SdxEvent::getResourceId)
                .returns(USER_ID, SdxEvent::getUserId)
                .returns(exception, DatalakeRestartServicesFailedEvent::getException);
    }

    @Test
    void doAcceptPollerStoppedException() {
        PollerStoppedException exception = new PollerStoppedException("exception");
        doThrow(exception).when(poller).pollServiceRestartUntilAvailable(any(), any());

        DatalakeRestartServicesFailedEvent result = (DatalakeRestartServicesFailedEvent) underTest.doAccept(event);

        assertThat(result)
                .returns(SDX_ID, SdxEvent::getResourceId)
                .returns(USER_ID, SdxEvent::getUserId);
        assertThat(result.getException())
                .isInstanceOf(PollerStoppedException.class)
                .hasMessage("Datalake restart services timed out after " + DURATION_IN_MINUTES + " minutes");
    }

    @Test
    void doAcceptPollerException() {
        PollerException exception = new PollerException("exception");
        doThrow(exception).when(poller).pollServiceRestartUntilAvailable(any(), any());

        DatalakeRestartServicesFailedEvent result = (DatalakeRestartServicesFailedEvent) underTest.doAccept(event);

        assertThat(result)
                .returns(SDX_ID, SdxEvent::getResourceId)
                .returns(USER_ID, SdxEvent::getUserId)
                .returns(exception, DatalakeRestartServicesFailedEvent::getException);
    }

    private void verifyPollingConfig() {
        ArgumentCaptor<PollingConfig> pollingConfigArgumentCaptor = ArgumentCaptor.forClass(PollingConfig.class);
        verify(poller).pollServiceRestartUntilAvailable(eq(sdxCluster), pollingConfigArgumentCaptor.capture());
        PollingConfig pollingConfig = pollingConfigArgumentCaptor.getValue();
        assertEquals(SLEEP_TIME_IN_SEC, pollingConfig.getSleepTime());
        assertEquals(TimeUnit.SECONDS, pollingConfig.getSleepTimeUnit());
        assertEquals(DURATION_IN_MINUTES, pollingConfig.getDuration());
        assertEquals(TimeUnit.MINUTES, pollingConfig.getDurationTimeUnit());
    }

}
