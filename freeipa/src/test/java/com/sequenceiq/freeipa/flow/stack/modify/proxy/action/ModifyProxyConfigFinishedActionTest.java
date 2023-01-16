package com.sequenceiq.freeipa.flow.stack.modify.proxy.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.ActionTest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigContext;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

class ModifyProxyConfigFinishedActionTest extends ActionTest {

    private static final String OPERATION_ID = "operationId";

    private static final Map<Object, Object> VARIABLES = Map.of(OperationAwareAction.OPERATION_ID, OPERATION_ID);

    private static final String ACCOUNT_ID = "accountId";

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ModifyProxyConfigFinishedAction underTest;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private OperationService operationService;

    @Mock
    private ModifyProxyConfigContext context;

    @Mock
    private Stack stack;

    @BeforeEach
    void setUp() {
        super.setUp(context);
        lenient().when(context.getStack()).thenReturn(stack);
        lenient().when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(stack.getId()).thenReturn(STACK_ID);
    }

    @Test
    void doExecute() throws Exception {
        StackEvent stackEvent = new StackEvent(STACK_ID);

        underTest.doExecute(context, stackEvent, VARIABLES);

        verify(stackUpdater).updateStackStatus(stackEvent.getResourceId(), DetailedStackStatus.AVAILABLE,
                "Successfully updated proxy config settings on all instances");
        SuccessDetails successDetails = new SuccessDetails(context.getStack().getEnvironmentCrn());
        verify(operationService).completeOperation(ACCOUNT_ID, OPERATION_ID, Set.of(successDetails), Set.of());
        verifySendEvent();
    }

    @Test
    void createRequest() {
        Selectable result = underTest.createRequest(context);

        assertThat(result)
                .isInstanceOf(StackEvent.class)
                .extracting(StackEvent.class::cast)
                .returns(ModifyProxyConfigEvent.MODIFY_PROXY_FINISHED_EVENT.event(), StackEvent::selector)
                .returns(STACK_ID, StackEvent::getResourceId);
    }

}
