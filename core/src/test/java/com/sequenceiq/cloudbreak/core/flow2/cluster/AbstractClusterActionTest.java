package com.sequenceiq.cloudbreak.core.flow2.cluster;

import static org.mockito.Mockito.when;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@ExtendWith(MockitoExtension.class)
class AbstractClusterActionTest {

    private static final long STACK_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StateContext<FlowState, FlowEvent> clusterContext;

    @InjectMocks
    private final MockClusterAction underTest = new MockClusterAction();

    @Spy
    private ClusterView cluster;

    @Spy
    private StackView stack;

    @BeforeEach
    void setUp() {
        when(cluster.getName()).thenReturn("cluster-name");

        when(stack.getResourceCrn()).thenReturn("resource-crn");
        when(stack.getEnvironmentCrn()).thenReturn("env-crn");
        when(stack.getWorkspaceName()).thenReturn("workspace");

        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stack);
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(cluster);
    }

    @Test
    void createFlowContextShouldSetMdcContext() {
        underTest.createFlowContext(flowParameters, clusterContext, new StackEvent(STACK_ID));

        Assertions.assertThat(MDC.getMap())
                .containsEntry(LoggerContextKey.RESOURCE_TYPE.toString(), "CLUSTER")
                .containsEntry(LoggerContextKey.RESOURCE_NAME.toString(), cluster.getName())
                .containsEntry(LoggerContextKey.RESOURCE_CRN.toString(), stack.getResourceCrn())
                .containsEntry(LoggerContextKey.ENVIRONMENT_CRN.toString(), stack.getEnvironmentCrn());
    }

    static class MockClusterAction extends AbstractClusterAction<StackEvent> {

        protected MockClusterAction() {
            super(StackEvent.class);
        }

        @Override
        protected void doExecute(ClusterViewContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {

        }
    }
}