package com.sequenceiq.freeipa.flow.stack.modify.proxy.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.ActionTest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigContext;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

class ModifyProxyConfigFinishedActionTest extends ActionTest {

    private static final String OPERATION_ID = "operationId";

    private static final String ACCOUNT_ID = "accountId";

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ModifyProxyConfigFinishedAction underTest;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private OperationService operationService;

    private ModifyProxyConfigContext context;

    private Map<Object, Object> variables;

    @Mock
    private Stack stack;

    @BeforeEach
    void setUp() {
        context = new ModifyProxyConfigContext(flowParameters, stack);
        variables = new HashMap<>();
        lenient().when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(stack.getId()).thenReturn(STACK_ID);
    }

    @ParameterizedTest
    @MethodSource("doExecuteArguments")
    void doExecute(String operationId, boolean chained, boolean finalChain, boolean completeOperation) throws Exception {
        StackEvent stackEvent = new StackEvent(STACK_ID);
        underTest.setOperationId(variables, operationId);
        underTest.setChainedAction(variables, chained);
        underTest.setFinalChain(variables, finalChain);

        underTest.doExecute(context, stackEvent, variables);

        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.AVAILABLE,
                "Successfully updated proxy config settings on all instances");
        SuccessDetails successDetails = new SuccessDetails(context.getStack().getEnvironmentCrn());
        verify(operationService, completeOperation ? times(1) : never()).completeOperation(ACCOUNT_ID, OPERATION_ID, Set.of(successDetails), Set.of());
        verifySendEvent();
    }

    public static Stream<Arguments> doExecuteArguments() {
        return Stream.of(
                // no operation or flowchain
                Arguments.of(null, false, false, false),
                // operation outside flowchain
                Arguments.of(OPERATION_ID, false, false, true),
                // operation and non-last flow in flowchain
                Arguments.of(OPERATION_ID, true, false, false),
                // operation and last flow in flowchain
                Arguments.of(OPERATION_ID, true, true, true)
        );
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