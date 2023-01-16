package com.sequenceiq.datalake.flow.modifyproxy.handler;

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
import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigFailureResponse;
import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigSuccessResponse;
import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigWaitHandlerTest {

    private static final long SLEEP_TIME_IN_SEC = 90;

    private static final long DURATION_IN_MINUTES = 30;

    @Mock
    private SdxWaitService sdxWaitService;

    @InjectMocks
    private ModifyProxyConfigWaitHandler underTest;

    private ModifyProxyConfigWaitRequest request;

    @Captor
    private ArgumentCaptor<PollingConfig> pollingConfigArgumentCaptor;

    @BeforeEach
    void setUp() {
        request = new ModifyProxyConfigWaitRequest(1L, "user-id");
        ReflectionTestUtils.setField(underTest, "sleepTimeInSec", SLEEP_TIME_IN_SEC);
        ReflectionTestUtils.setField(underTest, "durationInMinutes", DURATION_IN_MINUTES);
    }

    @Test
    void doAcceptSuccess() {
        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertThat(result)
                .isInstanceOf(ModifyProxyConfigSuccessResponse.class)
                .extracting(ModifyProxyConfigSuccessResponse.class::cast)
                .returns(request.getResourceId(), ModifyProxyConfigSuccessResponse::getResourceId)
                .returns(request.getUserId(), ModifyProxyConfigSuccessResponse::getUserId);
        verify(sdxWaitService).waitForCloudbreakFlow(eq(request.getResourceId()), pollingConfigArgumentCaptor.capture(), eq("Modifying proxy config"));
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
                .isInstanceOf(ModifyProxyConfigFailureResponse.class)
                .extracting(ModifyProxyConfigFailureResponse.class::cast)
                .returns(request.getResourceId(), ModifyProxyConfigFailureResponse::getResourceId)
                .returns(request.getUserId(), ModifyProxyConfigFailureResponse::getUserId)
                .returns(cause, ModifyProxyConfigFailureResponse::getException);
    }

}
