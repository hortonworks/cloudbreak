package com.sequenceiq.datalake.flow.salt.rotatepassword.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.flow.salt.rotatepassword.event.RotateSaltPasswordFailureResponse;
import com.sequenceiq.datalake.flow.salt.rotatepassword.event.RotateSaltPasswordSuccessResponse;
import com.sequenceiq.datalake.flow.salt.rotatepassword.event.RotateSaltPasswordWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class RotateSaltPasswordWaitHandlerTest {

    private static final long SLEEP_TIME_IN_SEC = 90;

    private static final long DURATION_IN_MINUTES = 30;

    private static final String USER_ID = "user-id";

    private static final long SDX_ID = 1L;

    private static final HandlerEvent<RotateSaltPasswordWaitRequest> EVENT =
            new HandlerEvent<>(new Event<>(new RotateSaltPasswordWaitRequest(SDX_ID, USER_ID)));

    @Mock
    private SdxWaitService sdxWaitService;

    @InjectMocks
    private RotateSaltPasswordWaitHandler underTest;

    @Captor
    private ArgumentCaptor<PollingConfig> pollingConfigArgumentCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "sleepTimeInSec", SLEEP_TIME_IN_SEC);
        ReflectionTestUtils.setField(underTest, "durationInMinutes", DURATION_IN_MINUTES);
    }

    @Test
    void testSuccess() {
        Selectable result = underTest.doAccept(EVENT);

        assertThat(result)
                .isInstanceOf(RotateSaltPasswordSuccessResponse.class)
                .extracting(RotateSaltPasswordSuccessResponse.class::cast)
                .returns(SDX_ID, RotateSaltPasswordSuccessResponse::getResourceId)
                .returns(USER_ID, RotateSaltPasswordSuccessResponse::getUserId);
        verify(sdxWaitService).waitForCloudbreakFlow(eq(SDX_ID), pollingConfigArgumentCaptor.capture(), eq("Rotating SaltStack user password"));
        assertThat(pollingConfigArgumentCaptor.getValue())
                .returns(SLEEP_TIME_IN_SEC, PollingConfig::getSleepTime)
                .returns(TimeUnit.SECONDS, PollingConfig::getSleepTimeUnit)
                .returns(DURATION_IN_MINUTES, PollingConfig::getDuration)
                .returns(TimeUnit.MINUTES, PollingConfig::getDurationTimeUnit)
                .returns(true, PollingConfig::getStopPollingIfExceptionOccurred);
    }

    @Test
    void testFailure() {
        SdxWaitException sdxWaitException = new SdxWaitException("message", new Throwable());
        doThrow(sdxWaitException).when(sdxWaitService).waitForCloudbreakFlow(anyLong(), any(), anyString());

        Selectable result = underTest.doAccept(EVENT);

        assertThat(result)
                .isInstanceOf(RotateSaltPasswordFailureResponse.class)
                .extracting(RotateSaltPasswordFailureResponse.class::cast)
                .returns(SDX_ID, RotateSaltPasswordFailureResponse::getResourceId)
                .returns(USER_ID, RotateSaltPasswordFailureResponse::getUserId)
                .returns(sdxWaitException, RotateSaltPasswordFailureResponse::getException);
        verify(sdxWaitService).waitForCloudbreakFlow(eq(SDX_ID), any(), eq("Rotating SaltStack user password"));
    }
}
