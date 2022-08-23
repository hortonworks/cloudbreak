package com.sequenceiq.datalake.flow.create.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.flow.create.event.StorageConsumptionCollectionSchedulingRequest;
import com.sequenceiq.datalake.flow.create.event.StorageConsumptionCollectionSchedulingSuccessEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.consumption.ConsumptionService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class StorageConsumptionCollectionSchedulingHandlerTest {

    private static final long DATALAKE_ID = 12L;

    private static final String USER_ID = "userId";

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String DATALAKE_CRN = "datalakeCrn";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private ConsumptionService consumptionService;

    @InjectMocks
    private StorageConsumptionCollectionSchedulingHandler underTest;

    @Mock
    private Event<StorageConsumptionCollectionSchedulingRequest> event;

    @Mock
    private HandlerEvent<StorageConsumptionCollectionSchedulingRequest> handlerEvent;

    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @BeforeEach
    void setUp() {
        detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        lenient().when(handlerEvent.getData()).thenReturn(new StorageConsumptionCollectionSchedulingRequest(DATALAKE_ID, USER_ID, detailedEnvironmentResponse));
    }

    @Test
    void selectorTest() {
        assertThat(underTest.selector()).isEqualTo("STORAGECONSUMPTIONCOLLECTIONSCHEDULINGREQUEST");
    }

    @Test
    void defaultFailureEventTest() {
        UnsupportedOperationException exception = new UnsupportedOperationException("Bang!");

        Selectable result = underTest.defaultFailureEvent(DATALAKE_ID, exception, event);

        verifyFailedEvent(result, exception);
    }

    private void verifyFailedEvent(Selectable result, Exception exceptionExpected) {
        assertThat(result).isInstanceOf(SdxCreateFailedEvent.class);

        SdxCreateFailedEvent sdxCreateFailedEvent = (SdxCreateFailedEvent) result;
        assertThat(sdxCreateFailedEvent.getResourceId()).isEqualTo(DATALAKE_ID);
        assertThat(sdxCreateFailedEvent.getUserId()).isEqualTo(null);
        assertThat(sdxCreateFailedEvent.getException()).isSameAs(exceptionExpected);
        assertThat(sdxCreateFailedEvent.getSdxName()).isNull();
    }

    @Test
    void doAcceptTestErrorWhenSdxClusterAbsent() {
        when(sdxClusterRepository.findById(DATALAKE_ID)).thenReturn(Optional.empty());

        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> underTest.doAccept(handlerEvent));

        assertThat(notFoundException).hasMessage("SDX cluster '12' not found.");
        verify(consumptionService, never()).scheduleStorageConsumptionCollectionIfNeeded(any(SdxCluster.class));
    }

    @Test
    void doAcceptTestSuccess() {
        SdxCluster sdxCluster = sdxCluster();
        when(sdxClusterRepository.findById(DATALAKE_ID)).thenReturn(Optional.of(sdxCluster));

        Selectable result = underTest.doAccept(handlerEvent);

        verifySuccessEvent(result);
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
        assertThat(result).isInstanceOf(StorageConsumptionCollectionSchedulingSuccessEvent.class);

        StorageConsumptionCollectionSchedulingSuccessEvent schedulingSuccessEvent = (StorageConsumptionCollectionSchedulingSuccessEvent) result;
        assertThat(schedulingSuccessEvent.getResourceId()).isEqualTo(DATALAKE_ID);
        assertThat(schedulingSuccessEvent.getUserId()).isEqualTo(USER_ID);
        assertThat(schedulingSuccessEvent.getDetailedEnvironmentResponse()).isEqualTo(detailedEnvironmentResponse);
        assertThat(schedulingSuccessEvent.getSdxName()).isNull();
    }

}