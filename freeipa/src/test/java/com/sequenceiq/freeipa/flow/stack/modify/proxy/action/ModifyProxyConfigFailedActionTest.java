package com.sequenceiq.freeipa.flow.stack.modify.proxy.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.ActionTest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigContext;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigFailedActionTest extends ActionTest {

    private static final String OPERATION_ID = "operationId";

    private static final Map<Object, Object> VARIABLES = Map.of(OperationAwareAction.OPERATION_ID, OPERATION_ID);

    private static final String ACCOUNT_ID = "accountId";

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ModifyProxyConfigFailedAction underTest;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private OperationService operationService;

    @Mock
    private StackService stackService;

    private ModifyProxyConfigContext context;

    @Mock
    private Stack stack;

    @BeforeEach
    void setUp() {
        context = new ModifyProxyConfigContext(flowParameters, stack);
        lenient().when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(stack.getId()).thenReturn(STACK_ID);
    }

    @Test
    void createFlowContext() {
        Exception cause = new Exception("cause");
        StackFailureEvent payload = mock(StackFailureEvent.class);
        when(payload.getResourceId()).thenReturn(STACK_ID);

        underTest.createFlowContext(flowParameters, mock(StateContext.class), payload);

        verify(stackService).getStackById(STACK_ID);
    }

    @Test
    void doExecute() throws Exception {
        Exception exception = new Exception("cause");
        StackFailureEvent stackFailureEvent = new StackFailureEvent(STACK_ID, exception, ERROR);

        underTest.doExecute(context, stackFailureEvent, VARIABLES);

        verify(stackUpdater)
                .updateStackStatus(stack, DetailedStackStatus.MODIFY_PROXY_CONFIG_FAILED, exception.getMessage());
        verify(operationService).failOperation(ACCOUNT_ID, OPERATION_ID, exception.getMessage());
        verifySendEvent();
    }

    @Test
    void createRequest() {
        Selectable result = underTest.createRequest(context);

        assertThat(result)
                .isInstanceOf(StackEvent.class)
                .extracting(StackEvent.class::cast)
                .returns(ModifyProxyConfigEvent.MODIFY_PROXY_FAILURE_HANDLED_EVENT.event(), StackEvent::selector)
                .returns(STACK_ID, StackEvent::getResourceId);
    }

}