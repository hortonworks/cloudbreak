package com.sequenceiq.cloudbreak.reactor.handler.consumption;

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

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionUnschedulingFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionUnschedulingRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionUnschedulingSuccess;
import com.sequenceiq.cloudbreak.service.consumption.ConsumptionService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class AttachedVolumeConsumptionCollectionUnschedulingHandlerTest {

    private static final long STACK_ID = 12L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ConsumptionService consumptionService;

    @InjectMocks
    private AttachedVolumeConsumptionCollectionUnschedulingHandler underTest;

    @Mock
    private Event<AttachedVolumeConsumptionCollectionUnschedulingRequest> event;

    @Mock
    private HandlerEvent<AttachedVolumeConsumptionCollectionUnschedulingRequest> handlerEvent;

    @BeforeEach
    void setUp() {
        lenient().when(handlerEvent.getData()).thenReturn(new AttachedVolumeConsumptionCollectionUnschedulingRequest(STACK_ID));
    }

    @Test
    void selectorTest() {
        assertThat(underTest.selector()).isEqualTo("ATTACHEDVOLUMECONSUMPTIONCOLLECTIONUNSCHEDULINGREQUEST");
    }

    @Test
    void defaultFailureEventTest() {
        UnsupportedOperationException exception = new UnsupportedOperationException("Bang!");

        Selectable result = underTest.defaultFailureEvent(STACK_ID, exception, event);

        verifyFailedEvent(result, exception);
    }

    private void verifyFailedEvent(Selectable result, Exception exceptionExpected) {
        assertThat(result).isInstanceOf(AttachedVolumeConsumptionCollectionUnschedulingFailed.class);

        AttachedVolumeConsumptionCollectionUnschedulingFailed schedulingFailedEvent = (AttachedVolumeConsumptionCollectionUnschedulingFailed) result;
        assertThat(schedulingFailedEvent.getResourceId()).isEqualTo(STACK_ID);
        assertThat(schedulingFailedEvent.getException()).isSameAs(exceptionExpected);
    }

    @Test
    void doAcceptTestSuccess() {
        StackDto stackDto = new StackDto();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);

        Selectable result = underTest.doAccept(handlerEvent);

        verifySuccessEvent(result);
        verify(consumptionService).unscheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto);
    }

    private void verifySuccessEvent(Selectable result) {
        assertThat(result).isInstanceOf(AttachedVolumeConsumptionCollectionUnschedulingSuccess.class);

        AttachedVolumeConsumptionCollectionUnschedulingSuccess schedulingSuccessEvent = (AttachedVolumeConsumptionCollectionUnschedulingSuccess) result;
        assertThat(schedulingSuccessEvent.getResourceId()).isEqualTo(STACK_ID);
    }

}