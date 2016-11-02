package com.sequenceiq.cloudbreak.orchestrator.swarm;


import static com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.createRunner;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.exitCriteria;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.exitCriteriaModel;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.gatewayConfig;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.generateNodes;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.parallelContainerRunner;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.MunchausenBootstrap;

@RunWith(MockitoJUnitRunner.class)
public class SwarmContainerOrchestratorTest {

    private static final int FIX_NODE_COUNT = 10;

    private static final int FIX_CONSUL_SERVER_COUNT = 3;

    private static final int ZERO = 0;

    private static final int ONE = 1;

    private static final int TWO = 2;

    private static final int THREE = 3;

    private SwarmContainerOrchestrator underTest = new SwarmContainerOrchestrator();

    private SwarmContainerOrchestrator underTestSpy;

    @Mock
    private MunchausenBootstrap munchausenBootstrap;

    @Mock
    private Future<Boolean> future;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        underTest.init(parallelContainerRunner(), exitCriteria());
        underTestSpy = spy(underTest);
        doReturn(parallelContainerRunner()).when(underTestSpy).getParallelOrchestratorComponentRunner();
        when(underTestSpy.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class), anyMap())).thenAnswer(
                new Answer<Callable<Boolean>>() {
                    @Override
                    public Callable<Boolean> answer(InvocationOnMock invocation) {
                        Object[] arguments = invocation.getArguments();
                        OrchestratorBootstrap orchestratorBootstrap = (OrchestratorBootstrap) arguments[ZERO];
                        ExitCriteria exitCriteria = (ExitCriteria) arguments[ONE];
                        ExitCriteriaModel exitCriteriaModel = (ExitCriteriaModel) arguments[TWO];
                        Map<String, String> map = (Map<String, String>) arguments[THREE];
                        return createRunner(orchestratorBootstrap, exitCriteria, exitCriteriaModel, map);
                    }
                }
        );
    }

    @Test
    public void bootstrapClusterWhenEverythingWorksFine() throws Exception {
        when(munchausenBootstrap.call()).thenReturn(true);
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrap(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), FIX_CONSUL_SERVER_COUNT,
                exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void bootstrapClusterWhenOrchestratorCancelled() throws Exception {
        when(munchausenBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrap(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), FIX_CONSUL_SERVER_COUNT,
                exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void bootstrapClusterWhenOrchestratorFailed() throws Exception {
        when(munchausenBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrap(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), FIX_CONSUL_SERVER_COUNT,
                exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void bootstrapClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        when(munchausenBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrap(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), FIX_CONSUL_SERVER_COUNT,
                exitCriteriaModel());
    }

    @Test
    public void bootstrapNewNodesInClusterWhenEverythingWorksFine() throws Exception {
        when(munchausenBootstrap.call()).thenReturn(true);
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenNewNodeBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrapNewNodes(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void bootstrapNewNodesInClusterWhenOrchestratorCancelled() throws Exception {
        when(munchausenBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenNewNodeBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrapNewNodes(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void bootstrapNewNodesInClusterWhenOrchestratorFailed() throws Exception {
        when(munchausenBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenNewNodeBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrapNewNodes(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void bootstrapNewNodesInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        when(munchausenBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenNewNodeBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrapNewNodes(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), exitCriteriaModel());
    }
}