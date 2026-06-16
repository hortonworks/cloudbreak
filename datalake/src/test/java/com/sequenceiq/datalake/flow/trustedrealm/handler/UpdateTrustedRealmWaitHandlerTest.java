package com.sequenceiq.datalake.flow.trustedrealm.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.sequenceiq.datalake.flow.trustedrealm.event.UpdateTrustedRealmFailureResponse;
import com.sequenceiq.datalake.flow.trustedrealm.event.UpdateTrustedRealmSuccessResponse;
import com.sequenceiq.datalake.flow.trustedrealm.event.UpdateTrustedRealmWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class UpdateTrustedRealmWaitHandlerTest {

    private static final long SLEEP_TIME_IN_SEC = 10;

    private static final long DURATION_IN_MINUTES = 30;

    @Mock
    private SdxWaitService sdxWaitService;

    @InjectMocks
    private UpdateTrustedRealmWaitHandler underTest;

    private UpdateTrustedRealmWaitRequest request;

    @Captor
    private ArgumentCaptor<PollingConfig> pollingConfigArgumentCaptor;

    @BeforeEach
    void setUp() {
        request = new UpdateTrustedRealmWaitRequest(1L, "user-id");
        ReflectionTestUtils.setField(underTest, "sleepTimeInSec", SLEEP_TIME_IN_SEC);
        ReflectionTestUtils.setField(underTest, "durationInMinutes", DURATION_IN_MINUTES);
    }

    @Test
    void doAcceptSuccess() {
        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertThat(result)
                .isInstanceOf(UpdateTrustedRealmSuccessResponse.class)
                .extracting(UpdateTrustedRealmSuccessResponse.class::cast)
                .returns(request.getResourceId(), UpdateTrustedRealmSuccessResponse::getResourceId)
                .returns(request.getUserId(), UpdateTrustedRealmSuccessResponse::getUserId);
        verify(sdxWaitService).waitForCloudbreakFlow(eq(request.getResourceId()), pollingConfigArgumentCaptor.capture(), eq("Updating trusted realm"));
        assertThat(pollingConfigArgumentCaptor.getValue())
                .returns(SLEEP_TIME_IN_SEC, PollingConfig::getSleepTime)
                .returns(TimeUnit.SECONDS, PollingConfig::getSleepTimeUnit)
                .returns(DURATION_IN_MINUTES, PollingConfig::getDuration)
                .returns(TimeUnit.MINUTES, PollingConfig::getDurationTimeUnit)
                .returns(true, PollingConfig::getStopPollingIfExceptionOccurred);
    }

    @Test
    void doAcceptFailure() {
        SdxWaitException cause = new SdxWaitException("cause", new Exception());
        doThrow(cause).when(sdxWaitService).waitForCloudbreakFlow(anyLong(), any(), any());

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertThat(result)
                .isInstanceOf(UpdateTrustedRealmFailureResponse.class)
                .extracting(UpdateTrustedRealmFailureResponse.class::cast)
                .returns(request.getResourceId(), UpdateTrustedRealmFailureResponse::getResourceId)
                .returns(request.getUserId(), UpdateTrustedRealmFailureResponse::getUserId)
                .returns(cause, UpdateTrustedRealmFailureResponse::getException);
    }

}
