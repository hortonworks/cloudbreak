package com.sequenceiq.datalake.flow.delete.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.delete.event.SdxDeletionFailedEvent;
import com.sequenceiq.datalake.flow.delete.event.StorageConsumptionCollectionUnschedulingRequest;
import com.sequenceiq.datalake.flow.delete.event.StorageConsumptionCollectionUnschedulingSuccessEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.consumption.ConsumptionService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class StorageConsumptionCollectionUnschedulingHandlerTest {

    private static final long DATALAKE_ID = 12L;

    private static final String USER_ID = "userId";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private ConsumptionService consumptionService;

    @InjectMocks
    private StorageConsumptionCollectionUnschedulingHandler underTest;

    @Mock
    private Event<StorageConsumptionCollectionUnschedulingRequest> event;

    @Mock
    private HandlerEvent<StorageConsumptionCollectionUnschedulingRequest> handlerEvent;

    @Test
    void selectorTest() {
        assertThat(underTest.selector()).isEqualTo("STORAGECONSUMPTIONCOLLECTIONUNSCHEDULINGREQUEST");
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void defaultFailureEventTest(boolean forceDelete) {
        UnsupportedOperationException exception = new UnsupportedOperationException("Bang!");
        when(event.getData()).thenReturn(new StorageConsumptionCollectionUnschedulingRequest(DATALAKE_ID, USER_ID, forceDelete));

        Selectable result = underTest.defaultFailureEvent(DATALAKE_ID, exception, event);

        verifyFailedEvent(result, exception, forceDelete);
    }

    private void verifyFailedEvent(Selectable result, Exception exceptionExpected, boolean forceDeleteExpected) {
        assertThat(result).isInstanceOf(SdxDeletionFailedEvent.class);

        SdxDeletionFailedEvent sdxDeletionFailedEvent = (SdxDeletionFailedEvent) result;
        assertThat(sdxDeletionFailedEvent.getResourceId()).isEqualTo(DATALAKE_ID);
        assertThat(sdxDeletionFailedEvent.getUserId()).isEqualTo(null);
        assertThat(sdxDeletionFailedEvent.getException()).isSameAs(exceptionExpected);
        assertThat(sdxDeletionFailedEvent.getSdxName()).isNull();
        assertThat(sdxDeletionFailedEvent.isForced()).isEqualTo(forceDeleteExpected);
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void doAcceptTestSkipConsumptionWhenSdxClusterAbsent(boolean forceDelete) {
        initHandlerEvent(forceDelete);
        when(sdxClusterRepository.findById(DATALAKE_ID)).thenReturn(Optional.empty());

        Selectable result = underTest.doAccept(handlerEvent);

        verifySuccessEvent(result, forceDelete);
        verify(consumptionService, never()).unscheduleStorageConsumptionCollectionIfNeeded(any(SdxCluster.class));
    }

    private void initHandlerEvent(boolean forceDelete) {
        when(handlerEvent.getData()).thenReturn(new StorageConsumptionCollectionUnschedulingRequest(DATALAKE_ID, USER_ID, forceDelete));
    }

    private void verifySuccessEvent(Selectable result, boolean forceDeleteExpected) {
        assertThat(result).isInstanceOf(StorageConsumptionCollectionUnschedulingSuccessEvent.class);

        StorageConsumptionCollectionUnschedulingSuccessEvent rdsDeletionSuccessEvent = (StorageConsumptionCollectionUnschedulingSuccessEvent) result;
        assertThat(rdsDeletionSuccessEvent.getResourceId()).isEqualTo(DATALAKE_ID);
        assertThat(rdsDeletionSuccessEvent.getUserId()).isEqualTo(USER_ID);
        assertThat(rdsDeletionSuccessEvent.getSdxName()).isNull();
        assertThat(rdsDeletionSuccessEvent.isForced()).isEqualTo(forceDeleteExpected);
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void doAcceptTestExecuteConsumption(boolean forceDelete) {
        initHandlerEvent(forceDelete);
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxClusterRepository.findById(DATALAKE_ID)).thenReturn(Optional.of(sdxCluster));

        Selectable result = underTest.doAccept(handlerEvent);

        verifySuccessEvent(result, forceDelete);
        verify(consumptionService).unscheduleStorageConsumptionCollectionIfNeeded(sdxCluster);
    }

}