package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.action;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.core.FlowParameters;

@ExtendWith(MockitoExtension.class)
class AbstractExternalDatabaseStopActionTest {

    @Mock
    private StackService stackService;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StateContext<ExternalDatabaseStopState, ExternalDatabaseStopEvent> stateContext;

    @InjectMocks
    private final AbstractExternalDatabaseStopAction<TestPayload> underTest = new TestAction(TestPayload.class);

    private boolean semaphore;

    @Test
    void createFlowContext() {
        Stack stack = new Stack();
        Workspace workspace = new Workspace();
        workspace.setTenant(new Tenant());
        stack.setWorkspace(workspace);
        when(stackService.getByIdWithClusterInTransaction(1L)).thenReturn(stack);

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

    private class TestAction extends AbstractExternalDatabaseStopAction<TestPayload> {

        TestAction(Class<TestPayload> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected void doExecute(ExternalDatabaseContext context, TestPayload payload, Map<Object, Object> variables) {
        }

        @Override
        protected void beforeReturnFlowContext(FlowParameters flowParameters,
                StateContext<ExternalDatabaseStopState, ExternalDatabaseStopEvent> stateContext, TestPayload payload) {

            super.beforeReturnFlowContext(flowParameters, stateContext, payload);
            semaphore = true;
        }
    }

}
