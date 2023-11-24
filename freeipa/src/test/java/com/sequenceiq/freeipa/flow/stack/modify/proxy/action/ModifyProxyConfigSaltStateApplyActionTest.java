package com.sequenceiq.freeipa.flow.stack.modify.proxy.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.core.ActionTest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigContext;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.event.ModifyProxyConfigSaltStateApplyRequest;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.event.ModifyProxyConfigTriggerEvent;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigSaltStateApplyActionTest extends ActionTest {

    private static final String OPERATION_ID = "operationId";

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ModifyProxyConfigSaltStateApplyAction underTest;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private ModifyProxyConfigContext context;

    @Mock
    private Stack stack;

    private ModifyProxyConfigTriggerEvent payload;

    @BeforeEach
    void setUp() {
        context = new ModifyProxyConfigContext(flowParameters, stack);
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        payload = new ModifyProxyConfigTriggerEvent(STACK_ID, mock(Promise.class), true, false, OPERATION_ID);
    }

    @Test
    void doExecute() throws Exception {
        Map<Object, Object> variables = new HashMap<>();

        underTest.doExecute(context, payload, variables);

        assertThat(underTest.isChainedAction(variables)).isTrue();
        assertThat(underTest.isFinalChain(variables)).isFalse();
        assertThat(underTest.getOperationId(variables)).isEqualTo(OPERATION_ID);
        verify(stackUpdater).updateStackStatus(STACK_ID, DetailedStackStatus.MODIFY_PROXY_CONFIG_IN_PROGRESS,
                "Applying modified proxy config salt state");
        assertThat(variables).containsEntry(OperationAwareAction.OPERATION_ID, OPERATION_ID);
        verifySendEvent();
    }

    @Test
    void createRequest() {
        Selectable result = underTest.createRequest(context);

        assertThat(result)
                .isInstanceOf(ModifyProxyConfigSaltStateApplyRequest.class)
                .extracting(ModifyProxyConfigSaltStateApplyRequest.class::cast)
                .returns(STACK_ID, ModifyProxyConfigSaltStateApplyRequest::getResourceId);
    }

}
