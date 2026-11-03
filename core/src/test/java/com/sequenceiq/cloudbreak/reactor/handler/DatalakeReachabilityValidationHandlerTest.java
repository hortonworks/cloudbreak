package com.sequenceiq.cloudbreak.reactor.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.DatalakeReachabilityValidationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.DatalakeReachabilityValidationResult;
import com.sequenceiq.cloudbreak.service.datalake.DataLakeStatusCheckerService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeNotReachableException;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DatalakeReachabilityValidationHandlerTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private DatalakeReachabilityValidationHandler underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private DataLakeStatusCheckerService dataLakeStatusCheckerService;

    @Mock
    private StackView stackView;

    @Test
    void testDatalakeReachableThenSuccess() {
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);

        Selectable result = underTest.doAccept(getHandlerEvent());

        verify(dataLakeStatusCheckerService).validateDatalakeReachable(stackView);
        assertInstanceOf(DatalakeReachabilityValidationResult.class, result);
        assertEquals(EventSelectorUtil.selector(DatalakeReachabilityValidationResult.class), result.selector());
    }

    @Test
    void testDatalakeNotReachableThenExceptionIsPropagated() {
        DatalakeNotReachableException exception = new DatalakeNotReachableException("Data Lake is not reachable", new RuntimeException());
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        doThrow(exception).when(dataLakeStatusCheckerService).validateDatalakeReachable(stackView);

        DatalakeNotReachableException actual = assertThrows(DatalakeNotReachableException.class, () -> underTest.doAccept(getHandlerEvent()));

        assertEquals(exception, actual);
    }

    @Test
    void testDefaultFailureEvent() {
        DatalakeNotReachableException exception = new DatalakeNotReachableException("Data Lake is not reachable", new RuntimeException());

        Selectable result = underTest.defaultFailureEvent(STACK_ID, exception, new Event<>(new DatalakeReachabilityValidationRequest(STACK_ID)));

        assertEquals(EventSelectorUtil.failureSelector(DatalakeReachabilityValidationResult.class), result.selector());
        assertEquals(exception, ((StackFailureEvent) result).getException());
    }

    private HandlerEvent<DatalakeReachabilityValidationRequest> getHandlerEvent() {
        HandlerEvent<DatalakeReachabilityValidationRequest> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getData()).thenReturn(new DatalakeReachabilityValidationRequest(STACK_ID));
        return handlerEvent;
    }
}
