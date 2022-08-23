package com.sequenceiq.datalake.flow.create.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.create.event.EnvWaitRequest;
import com.sequenceiq.datalake.flow.create.event.EnvWaitSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class EnvWaitHandlerTest {

    private static final int DURATION_IN_MINUTES = 15;

    private static final long DATALAKE_ID = 12L;

    private static final String USER_ID = "userId";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private EnvWaitHandler underTest;

    @Mock
    private Event<EnvWaitRequest> event;

    @Mock
    private HandlerEvent<EnvWaitRequest> handlerEvent;

    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "durationInMinutes", DURATION_IN_MINUTES);

        lenient().when(handlerEvent.getData()).thenReturn(new EnvWaitRequest(DATALAKE_ID, USER_ID));

        detailedEnvironmentResponse = new DetailedEnvironmentResponse();
    }

    @Test
    void selectorTest() {
        assertThat(underTest.selector()).isEqualTo("EnvWaitRequest");
    }

    @Test
    void defaultFailureEventTest() {
        UnsupportedOperationException exception = new UnsupportedOperationException("Bang!");

        Selectable result = underTest.defaultFailureEvent(DATALAKE_ID, exception, event);

        verifyFailedEvent(result, null, exception);
    }

    private void verifyFailedEvent(Selectable result, String userIdExpected, Exception exceptionExpected) {
        verifyFailedEventInternal(result, userIdExpected, exceptionExpected, null, null);
    }

    private <E extends Exception> void verifyFailedEvent(Selectable result, String userIdExpected, Class<E> exceptionClassExpected,
            String exceptionMessageExpected) {
        verifyFailedEventInternal(result, userIdExpected, null, exceptionClassExpected, exceptionMessageExpected);
    }

    private <E extends Exception> void verifyFailedEventInternal(Selectable result, String userIdExpected, Exception exceptionExpected,
            Class<E> exceptionClassExpected, String exceptionMessageExpected) {
        assertThat(result).isInstanceOf(SdxCreateFailedEvent.class);

        SdxCreateFailedEvent sdxCreateFailedEvent = (SdxCreateFailedEvent) result;
        assertThat(sdxCreateFailedEvent.getResourceId()).isEqualTo(DATALAKE_ID);
        assertThat(sdxCreateFailedEvent.getUserId()).isEqualTo(userIdExpected);
        if (exceptionExpected != null) {
            assertThat(sdxCreateFailedEvent.getException()).isSameAs(exceptionExpected);
        } else {
            assertThat(sdxCreateFailedEvent.getException()).isInstanceOf(exceptionClassExpected);
            assertThat(sdxCreateFailedEvent.getException()).hasMessage(exceptionMessageExpected);
        }
        assertThat(sdxCreateFailedEvent.getSdxName()).isNull();
    }

    @Test
    void doAcceptTestErrorWhenUserBreakException() {
        UserBreakException userBreakException = new UserBreakException("Problem");
        when(environmentService.waitAndGetEnvironment(DATALAKE_ID)).thenThrow(userBreakException);

        Selectable result = underTest.doAccept(handlerEvent);

        verifyFailedEvent(result, USER_ID, userBreakException);
    }

    @Test
    void doAcceptTestErrorWhenPollerStoppedException() {
        PollerStoppedException pollerStoppedException = new PollerStoppedException("Problem");
        when(environmentService.waitAndGetEnvironment(DATALAKE_ID)).thenThrow(pollerStoppedException);

        Selectable result = underTest.doAccept(handlerEvent);

        verifyFailedEvent(result, USER_ID, PollerStoppedException.class, "Env wait timed out after 15 minutes");
    }

    @Test
    void doAcceptTestErrorWhenPollerException() {
        PollerException pollerException = new PollerException("Problem");
        when(environmentService.waitAndGetEnvironment(DATALAKE_ID)).thenThrow(pollerException);

        Selectable result = underTest.doAccept(handlerEvent);

        verifyFailedEvent(result, USER_ID, pollerException);
    }

    @Test
    void doAcceptTestErrorWhenOtherException() {
        IllegalStateException exception = new IllegalStateException("Problem");
        when(environmentService.waitAndGetEnvironment(DATALAKE_ID)).thenThrow(exception);

        Selectable result = underTest.doAccept(handlerEvent);

        verifyFailedEvent(result, USER_ID, exception);
    }

    @Test
    void doAcceptTestSuccess() {
        when(environmentService.waitAndGetEnvironment(DATALAKE_ID)).thenReturn(detailedEnvironmentResponse);

        Selectable result = underTest.doAccept(handlerEvent);

        verifySuccessEvent(result);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.ENVIRONMENT_CREATED, "Environment created", DATALAKE_ID);
    }

    private void verifySuccessEvent(Selectable result) {
        assertThat(result).isInstanceOf(EnvWaitSuccessEvent.class);

        EnvWaitSuccessEvent envWaitSuccessEvent = (EnvWaitSuccessEvent) result;
        assertThat(envWaitSuccessEvent.getResourceId()).isEqualTo(DATALAKE_ID);
        assertThat(envWaitSuccessEvent.getUserId()).isEqualTo(USER_ID);
        assertThat(envWaitSuccessEvent.getDetailedEnvironmentResponse()).isEqualTo(detailedEnvironmentResponse);
        assertThat(envWaitSuccessEvent.getSdxName()).isNull();
    }

}