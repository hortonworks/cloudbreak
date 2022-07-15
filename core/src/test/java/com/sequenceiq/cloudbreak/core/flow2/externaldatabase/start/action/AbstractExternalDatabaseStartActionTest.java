package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseContext;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartState;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

@ExtendWith(MockitoExtension.class)
class AbstractExternalDatabaseStartActionTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StateContext<ExternalDatabaseStartState, ExternalDatabaseStartEvent> stateContext;

    @InjectMocks
    private AbstractExternalDatabaseStartAction<TestPayload> underTest = new TestAction(TestPayload.class);

    private boolean semaphore;

    @Test
    void createFlowContext() {
        StackView stack = mock(StackView.class);
        when(stack.getTenantName()).thenReturn("tenant");
        when(stack.getWorkspaceName()).thenReturn("ws");
        when(stackDtoService.getStackViewById(1L)).thenReturn(stack);

        semaphore = false;
        ExternalDatabaseContext flowContext = underTest.createFlowContext(flowParameters, stateContext, new TestPayload());

        assertThat(flowContext.getFlowParameters()).isEqualTo(flowParameters);
        assertThat(flowContext.getStack()).isEqualTo(stack);
        assertThat(semaphore).isTrue();
    }

    private class TestPayload implements Payload {

        @Override
        public Long getResourceId() {
            return 1L;
        }
    }

    private class TestAction extends AbstractExternalDatabaseStartAction<TestPayload> {

        TestAction(Class<TestPayload> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected void doExecute(ExternalDatabaseContext context, TestPayload payload, Map<Object, Object> variables) {
        }

        @Override
        protected void beforeReturnFlowContext(FlowParameters flowParameters,
                StateContext<ExternalDatabaseStartState, ExternalDatabaseStartEvent> stateContext, TestPayload payload) {

            super.beforeReturnFlowContext(flowParameters, stateContext, payload);
            semaphore = true;
        }
    }

}
