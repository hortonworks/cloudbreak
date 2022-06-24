package com.sequenceiq.datalake.flow.create.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.create.event.EnvWaitRequest;
import com.sequenceiq.datalake.flow.create.event.EnvWaitSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.consumption.ConsumptionService;
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

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String DATALAKE_CRN = "datalakeCrn";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private ConsumptionService consumptionService;

    private EnvWaitHandler underTest;

    @Mock
    private Event<EnvWaitRequest> event;

    @Mock
    private HandlerEvent<EnvWaitRequest> handlerEvent;

    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @BeforeEach
    void setUp() {
        underTest = new EnvWaitHandler(DURATION_IN_MINUTES, environmentService, sdxStatusService, sdxClusterRepository, consumptionService);

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
        verify(sdxClusterRepository, never()).findById(anyLong());
        verify(consumptionService, never()).scheduleStorageConsumptionCollectionIfNeeded(any(SdxCluster.class));
    }

    @Test
    void doAcceptTestErrorWhenPollerStoppedException() {
        PollerStoppedException pollerStoppedException = new PollerStoppedException("Problem");
        when(environmentService.waitAndGetEnvironment(DATALAKE_ID)).thenThrow(pollerStoppedException);

        Selectable result = underTest.doAccept(handlerEvent);

        verifyFailedEvent(result, USER_ID, PollerStoppedException.class, "Env wait timed out after 15 minutes");
        verify(sdxClusterRepository, never()).findById(anyLong());
        verify(consumptionService, never()).scheduleStorageConsumptionCollectionIfNeeded(any(SdxCluster.class));
    }

    @Test
    void doAcceptTestErrorWhenPollerException() {
        PollerException pollerException = new PollerException("Problem");
        when(environmentService.waitAndGetEnvironment(DATALAKE_ID)).thenThrow(pollerException);

        Selectable result = underTest.doAccept(handlerEvent);

        verifyFailedEvent(result, USER_ID, pollerException);
        verify(sdxClusterRepository, never()).findById(anyLong());
        verify(consumptionService, never()).scheduleStorageConsumptionCollectionIfNeeded(any(SdxCluster.class));
    }

    @Test
    void doAcceptTestErrorWhenOtherException() {
        IllegalStateException exception = new IllegalStateException("Problem");
        when(environmentService.waitAndGetEnvironment(DATALAKE_ID)).thenThrow(exception);

        Selectable result = underTest.doAccept(handlerEvent);

        verifyFailedEvent(result, USER_ID, exception);
        verify(sdxClusterRepository, never()).findById(anyLong());
        verify(consumptionService, never()).scheduleStorageConsumptionCollectionIfNeeded(any(SdxCluster.class));
    }

    @Test
    void doAcceptTestErrorWhenSdxClusterAbsent() {
        when(environmentService.waitAndGetEnvironment(DATALAKE_ID)).thenReturn(detailedEnvironmentResponse);
        when(sdxClusterRepository.findById(DATALAKE_ID)).thenReturn(Optional.empty());

        Selectable result = underTest.doAccept(handlerEvent);

        verifyFailedEvent(result, USER_ID, NotFoundException.class, "SDX cluster '12' not found.");
        verify(environmentService).waitAndGetEnvironment(DATALAKE_ID);
        verify(consumptionService, never()).scheduleStorageConsumptionCollectionIfNeeded(any(SdxCluster.class));
    }

    @Test
    void doAcceptTestExecuteConsumption() {
        when(environmentService.waitAndGetEnvironment(DATALAKE_ID)).thenReturn(detailedEnvironmentResponse);
        SdxCluster sdxCluster = sdxCluster();
        when(sdxClusterRepository.findById(DATALAKE_ID)).thenReturn(Optional.of(sdxCluster));

        Selectable result = underTest.doAccept(handlerEvent);

        verifySuccessEvent(result);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.ENVIRONMENT_CREATED, "Environment created", DATALAKE_ID);
        verify(consumptionService).scheduleStorageConsumptionCollectionIfNeeded(sdxCluster);
    }

    private SdxCluster sdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setAccountId(ACCOUNT_ID);
        sdxCluster.setEnvCrn(ENVIRONMENT_CRN);
        sdxCluster.setCrn(DATALAKE_CRN);
        return sdxCluster;
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