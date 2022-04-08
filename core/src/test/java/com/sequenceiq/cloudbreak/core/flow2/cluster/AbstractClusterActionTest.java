package com.sequenceiq.cloudbreak.core.flow2.cluster;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@ExtendWith(MockitoExtension.class)
class AbstractClusterActionTest {

    private static final long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StateContext<FlowState, FlowEvent> clusterContext;

    @InjectMocks
    private final MockClusterAction underTest = new MockClusterAction();

    private ClusterView clusterView;

    private StackView stackView;

    @BeforeEach
    void setUp() {
        clusterView = new ClusterView();
        clusterView.setId(2L);
        clusterView.setName("cluster-name");
        clusterView.setWorkspace(TestUtil.workspace(3L, "workspace"));

        stackView = TestUtil.stackView();
        stackView.setResourceCrn("resource-crn");
        stackView.setEnvironmentCrn("env-crn");
        stackView.setClusterView(clusterView);

        Mockito.when(stackService.getViewByIdWithoutAuth(STACK_ID)).thenReturn(stackView);
    }

    @Test
    void createFlowContextShouldSetMdcContext() {
        underTest.createFlowContext(flowParameters, clusterContext, new StackEvent(STACK_ID));

        Assertions.assertThat(MDC.getMap())
                .containsEntry(LoggerContextKey.RESOURCE_TYPE.toString(), "CLUSTER")
                .containsEntry(LoggerContextKey.RESOURCE_NAME.toString(), clusterView.getName())
                .containsEntry(LoggerContextKey.RESOURCE_CRN.toString(), stackView.getResourceCrn())
                .containsEntry(LoggerContextKey.ENVIRONMENT_CRN.toString(), stackView.getEnvironmentCrn());
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