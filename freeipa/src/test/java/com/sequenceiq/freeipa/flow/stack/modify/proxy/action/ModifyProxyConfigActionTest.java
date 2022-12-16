package com.sequenceiq.freeipa.flow.stack.modify.proxy.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigContext;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigState;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigActionTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private DummyModifyProxyConfigAction underTest;

    @Mock
    private StackService stackService;

    @Mock
    private Stack stack;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StateContext<ModifyProxyConfigState, ModifyProxyConfigEvent> stateContext;

    private StackEvent event;

    @BeforeEach
    void setUp() {
        event = new StackEvent(STACK_ID);
        lenient().when(stackService.getStackById(STACK_ID)).thenReturn(stack);
    }

    @Test
    void createFlowContext() {
        ModifyProxyConfigContext result = underTest.createFlowContext(flowParameters, stateContext, event);

        verify(stackService).getStackById(STACK_ID);
        assertThat(result)
                .returns(flowParameters, CommonContext::getFlowParameters)
                .returns(stack, ModifyProxyConfigContext::getStack);
    }

    @Test
    void getFailurePayload() {
        Exception cause = new Exception("cause");
        Object failurePayload = underTest.getFailurePayload(event, Optional.empty(), cause);

        assertThat(failurePayload)
                .isInstanceOf(StackFailureEvent.class)
                .extracting(StackFailureEvent.class::cast)
                .returns(ModifyProxyConfigEvent.MODIFY_PROXY_FAILED_EVENT.selector(), StackFailureEvent::selector)
                .returns(STACK_ID, StackFailureEvent::getResourceId)
                .returns(cause, StackFailureEvent::getException);
    }

    static class DummyModifyProxyConfigAction extends ModifyProxyConfigAction<StackEvent> {

        protected DummyModifyProxyConfigAction() {
            super(StackEvent.class);
        }

        @Override
        protected void doExecute(ModifyProxyConfigContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
            // do nothing
        }
    }

}