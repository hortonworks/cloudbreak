package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayService;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.selection.ChangePrimaryGatewaySelectionRequest;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.selection.ChangePrimaryGatewaySelectionSuccess;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class SelectionHandlerTest {
    private static final Long STACK_ID = 1L;

    private static final String INSTANCE_ID_1 = "i-1";

    private static final String INSTANCE_ID_2 = "i-2";

    @Mock
    private ChangePrimaryGatewayService changePrimaryGatewayService;

    @Mock
    private StackService stackService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private SelectionHandler underTest;

    @Test
    void testAcceptSuccess() {
        Stack stack = mock(Stack.class);
        Optional<String> formerPgw = Optional.of(INSTANCE_ID_1);
        List<String> instanceIds = List.of(INSTANCE_ID_1);
        ChangePrimaryGatewaySelectionRequest request = new ChangePrimaryGatewaySelectionRequest(STACK_ID, instanceIds);

        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(changePrimaryGatewayService.getPrimaryGatewayInstanceId(any())).thenReturn(formerPgw);
        when(changePrimaryGatewayService.selectNewPrimaryGatewayInstanceId(any(), any())).thenReturn(INSTANCE_ID_2);

        underTest.accept(new Event<>(request));

        verify(stackService).getByIdWithListsInTransaction(eq(STACK_ID));
        verify(changePrimaryGatewayService).getPrimaryGatewayInstanceId(eq(stack));
        verify(changePrimaryGatewayService).selectNewPrimaryGatewayInstanceId(eq(stack), eq(instanceIds));
        verify(eventBus).notify(eq(EventSelectorUtil.selector(ChangePrimaryGatewaySelectionSuccess.class)), any(Event.class));
    }

    @Test
    void testAcceptFailure() {
        Stack stack = mock(Stack.class);
        List<String> instanceIds = List.of(INSTANCE_ID_1);
        ChangePrimaryGatewaySelectionRequest request = new ChangePrimaryGatewaySelectionRequest(STACK_ID, instanceIds);

        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(changePrimaryGatewayService.getPrimaryGatewayInstanceId(any())).thenThrow(new RuntimeException("Expected exception"));

        underTest.accept(new Event<>(request));

        verify(stackService).getByIdWithListsInTransaction(eq(STACK_ID));
        verify(eventBus).notify(eq(EventSelectorUtil.selector(ChangePrimaryGatewayFailureEvent.class)), any(Event.class));
    }
}