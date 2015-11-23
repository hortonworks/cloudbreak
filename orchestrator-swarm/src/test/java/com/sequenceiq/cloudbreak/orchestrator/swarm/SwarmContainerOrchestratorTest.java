package com.sequenceiq.cloudbreak.orchestrator.swarm;


import static com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.containerOrchestratorCluster;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.createRunner;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.exitCriteria;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.exitCriteriaModel;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.gatewayConfig;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.generateLogVolume;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.generateNodes;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.OrchestratorTestUtil.parallelContainerRunner;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.AmbariAgentBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.AmbariServerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.AmbariServerDatabaseBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.ConsulWatchBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.KerberosServerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.LogrotateBootsrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.MunchausenBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.RegistratorBootstrap;

@RunWith(MockitoJUnitRunner.class)
public class SwarmContainerOrchestratorTest {

    private static final int FIX_NODE_COUNT = 10;
    private static final int FIX_CONSUL_SERVER_COUNT = 3;
    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int TWO = 2;
    private static final int THREE = 3;
    private static final String CONSUL_LOG_PATH = "/log/consul";

    private SwarmContainerOrchestrator underTest = new SwarmContainerOrchestrator();
    private SwarmContainerOrchestrator underTestSpy;

    @Mock
    private MunchausenBootstrap munchausenBootstrap;

    @Mock
    private RegistratorBootstrap registratorBootstrap;

    @Mock
    private AmbariServerBootstrap ambariServerBootstrap;

    @Mock
    private AmbariAgentBootstrap ambariAgentBootstrap;

    @Mock
    private AmbariServerDatabaseBootstrap ambariServerDatabaseBootstrap;

    @Mock
    private KerberosServerBootstrap kerberosServerBootstrap;

    @Mock
    private ConsulWatchBootstrap consulWatchBootstrap;

    @Mock
    private LogrotateBootsrap logrotateBootsrap;

    @Mock
    private Future<Boolean> future;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        underTest.init(parallelContainerRunner(), exitCriteria());
        underTestSpy = spy(underTest);
        doReturn(parallelContainerRunner()).when(underTestSpy).getParallelContainerRunner();
        when(underTestSpy.runner(any(ContainerBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class), anyMap())).thenAnswer(
                new Answer<Callable<Boolean>>() {
                    @Override
                    public Callable<Boolean> answer(InvocationOnMock invocation) {
                        Object[] arguments = invocation.getArguments();
                        ContainerBootstrap containerBootstrap = (ContainerBootstrap) arguments[ZERO];
                        ExitCriteria exitCriteria = (ExitCriteria) arguments[ONE];
                        ExitCriteriaModel exitCriteriaModel = (ExitCriteriaModel) arguments[TWO];
                        Map<String, String> map = (Map<String, String>) arguments[THREE];
                        return createRunner(containerBootstrap, exitCriteria, exitCriteriaModel, map);
                    }
                }
        );
    }

    @Test
    public void bootstrapClusterWhenEverythingWorksFine() throws Exception {
        when(munchausenBootstrap.call()).thenReturn(true);
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrap(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), FIX_CONSUL_SERVER_COUNT, CONSUL_LOG_PATH,
                exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void bootstrapClusterWhenOrchestratorCancelled() throws Exception {
        when(munchausenBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrap(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), FIX_CONSUL_SERVER_COUNT, CONSUL_LOG_PATH,
                exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void bootstrapClusterWhenOrchestratorFailed() throws Exception {
        when(munchausenBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrap(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), FIX_CONSUL_SERVER_COUNT, CONSUL_LOG_PATH,
                exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void bootstrapClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        when(munchausenBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrap(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), FIX_CONSUL_SERVER_COUNT, CONSUL_LOG_PATH,
                exitCriteriaModel());
    }

    @Test
    public void bootstrapNewNodesInClusterWhenEverythingWorksFine() throws Exception {
        when(munchausenBootstrap.call()).thenReturn(true);
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenNewNodeBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrapNewNodes(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), CONSUL_LOG_PATH,
                exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void bootstrapNewNodesInClusterWhenOrchestratorCancelled() throws Exception {
        when(munchausenBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenNewNodeBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrapNewNodes(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), CONSUL_LOG_PATH,
                exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void bootstrapNewNodesInClusterWhenOrchestratorFailed() throws Exception {
        when(munchausenBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenNewNodeBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrapNewNodes(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), CONSUL_LOG_PATH,
                exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void bootstrapNewNodesInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        when(munchausenBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenNewNodeBootstrap(any(GatewayConfig.class), any(String.class), any(String[].class));

        underTestSpy.bootstrapNewNodes(gatewayConfig(), new ContainerConfig("seq/a", "v1.10"), generateNodes(FIX_NODE_COUNT), CONSUL_LOG_PATH,
                exitCriteriaModel());
    }

    @Test
    public void registratorStartInClusterWhenEverythingWorksFine() throws Exception {
        when(registratorBootstrap.call()).thenReturn(true);
        doReturn(registratorBootstrap).when(underTestSpy).registratorBootstrap(any(GatewayConfig.class), anyString(), any(Node.class));

        underTestSpy.startRegistrator(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("registrator",
                "0.0.1"), exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void registratorStartInClusterWhenOrchestratorCancelled() throws Exception {
        when(registratorBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(registratorBootstrap).when(underTestSpy).registratorBootstrap(any(GatewayConfig.class), anyString(), any(Node.class));

        underTestSpy.startRegistrator(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("registrator",
                "0.0.1"), exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void registratorStartInClusterWhenOrchestratorFailed() throws Exception {
        when(registratorBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(registratorBootstrap).when(underTestSpy).registratorBootstrap(any(GatewayConfig.class), anyString(), any(Node.class));

        underTestSpy.startRegistrator(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("registrator",
                "0.0.1"), exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void registratorStartInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        when(registratorBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(registratorBootstrap).when(underTestSpy).registratorBootstrap(any(GatewayConfig.class), anyString(), any(Node.class));

        underTestSpy.startRegistrator(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("registrator",
                "0.0.1"), exitCriteriaModel());
    }

    @Test
    public void ambariServerStartInClusterWhenEverythingWorksFine() throws Exception {
        when(ambariServerBootstrap.call()).thenReturn(true);
        doReturn(ambariServerBootstrap).when(underTestSpy).ambariServerBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), anyString(), any(LogVolumePath.class));
        when(ambariServerDatabaseBootstrap.call()).thenReturn(true);
        doReturn(ambariServerDatabaseBootstrap).when(underTestSpy).ambariServerDatabaseBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), any(LogVolumePath.class));

        underTestSpy.startAmbariServer(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)),
                new ContainerConfig("serverdb", "0.0.1"), new ContainerConfig("serverdb", "0.0.1"), "azure", generateLogVolume(), false, exitCriteriaModel());
    }

    @Test
    public void ambariServerStartWithAgentInClusterWhenEverythingWorksFine() throws Exception {
        when(ambariServerBootstrap.call()).thenReturn(true);
        doReturn(ambariServerBootstrap).when(underTestSpy).ambariServerBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), anyString(), any(LogVolumePath.class));
        when(ambariAgentBootstrap.call()).thenReturn(true);
        doReturn(ambariAgentBootstrap).when(underTestSpy).ambariAgentBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), anyString(), anyString(), any(LogVolumePath.class));
        when(ambariServerDatabaseBootstrap.call()).thenReturn(true);
        doReturn(ambariServerDatabaseBootstrap).when(underTestSpy).ambariServerDatabaseBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), any(LogVolumePath.class));

        underTestSpy.startAmbariServer(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)),
                new ContainerConfig("serverdb", "0.0.1"), new ContainerConfig("serverdb", "0.0.1"), "azure", generateLogVolume(), true, exitCriteriaModel());

        verify(underTestSpy, times(1)).ambariAgentBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), anyString(), anyString(), any(LogVolumePath.class));
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void ambariServerStartInClusterWhenOrchestratorCancelled() throws Exception {
        when(ambariServerBootstrap.call()).thenReturn(true);
        doReturn(ambariServerBootstrap).when(underTestSpy).ambariServerBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), anyString(), any(LogVolumePath.class));
        when(ambariServerDatabaseBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(ambariServerDatabaseBootstrap).when(underTestSpy).ambariServerDatabaseBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), any(LogVolumePath.class));

        underTestSpy.startAmbariServer(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)),
                new ContainerConfig("serverdb", "0.0.1"), new ContainerConfig("serverdb", "0.0.1"), "azure", generateLogVolume(), false, exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void ambariServerStartInClusterWhenOrchestratorFailed() throws Exception {
        when(ambariServerBootstrap.call()).thenReturn(true);
        doReturn(ambariServerBootstrap).when(underTestSpy).ambariServerBootstrap(any(GatewayConfig.class), anyString(), any(Node.class),
                anyString(), any(LogVolumePath.class));
        when(ambariServerDatabaseBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("cancelled"));
        doReturn(ambariServerDatabaseBootstrap).when(underTestSpy).ambariServerDatabaseBootstrap(any(GatewayConfig.class), anyString(), any(Node.class),
                any(LogVolumePath.class));

        underTestSpy.startAmbariServer(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)),
                new ContainerConfig("serverdb", "0.0.1"), new ContainerConfig("serverdb", "0.0.1"), "azure", generateLogVolume(), false, exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void ambariServerStartInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        when(ambariServerBootstrap.call()).thenReturn(true);
        doReturn(ambariServerBootstrap).when(underTestSpy).ambariServerBootstrap(any(GatewayConfig.class), anyString(), any(Node.class),
                anyString(), any(LogVolumePath.class));
        when(ambariServerDatabaseBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(ambariServerDatabaseBootstrap).when(underTestSpy).ambariServerDatabaseBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), any(LogVolumePath.class));

        underTestSpy.startAmbariServer(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)),
                new ContainerConfig("serverdb", "0.0.1"), new ContainerConfig("serverdb", "0.0.1"), "azure", generateLogVolume(), false, exitCriteriaModel());
    }

    @Test
    public void ambariAgentStartInClusterWhenEverythingWorksFine() throws Exception {
        when(ambariAgentBootstrap.call()).thenReturn(true);
        doReturn(ambariAgentBootstrap).when(underTestSpy).ambariAgentBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), anyString(), anyString(), any(LogVolumePath.class));

        underTestSpy.startAmbariAgents(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("agent", "0.0.1"),
                "azure", generateLogVolume(), exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void ambariAgentStartInClusterWhenOrchestratorCancelled() throws Exception {
        when(ambariAgentBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(ambariAgentBootstrap).when(underTestSpy).ambariAgentBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), anyString(), anyString(), any(LogVolumePath.class));

        underTestSpy.startAmbariAgents(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("agent", "0.0.1"),
                "azure", generateLogVolume(), exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void ambariAgentStartInClusterWhenOrchestratorFailed() throws Exception {
        when(ambariAgentBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(ambariAgentBootstrap).when(underTestSpy).ambariAgentBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), anyString(), anyString(), any(LogVolumePath.class));

        underTestSpy.startAmbariAgents(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("agent", "0.0.1"),
                "azure", generateLogVolume(), exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void ambariAgentStartInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        when(ambariAgentBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(ambariAgentBootstrap).when(underTestSpy).ambariAgentBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), anyString(), anyString(), any(LogVolumePath.class));

        underTestSpy.startAmbariAgents(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("agent", "0.0.1"),
                "azure", generateLogVolume(), exitCriteriaModel());
    }

    @Test
    public void logRotateStartInClusterWhenEverythingWorksFine() throws Exception {
        when(logrotateBootsrap.call()).thenReturn(true);
        doReturn(logrotateBootsrap).when(underTestSpy).logrotateBootsrap(any(GatewayConfig.class), anyString(), any(Node.class), anyString());

        underTestSpy.startLogrotate(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("rotate", "0.0.1"),
                exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void logRotateStartInClusterWhenOrchestratorCancelled() throws Exception {
        when(logrotateBootsrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(logrotateBootsrap).when(underTestSpy).logrotateBootsrap(any(GatewayConfig.class), anyString(), any(Node.class), anyString());

        underTestSpy.startLogrotate(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("rotate", "0.0.1"),
                exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void logRotateStartInClusterWhenOrchestratorFailed() throws Exception {
        when(logrotateBootsrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(logrotateBootsrap).when(underTestSpy).logrotateBootsrap(any(GatewayConfig.class), anyString(), any(Node.class), anyString());

        underTestSpy.startLogrotate(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("rotate", "0.0.1"),
                exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void logRotateStartInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        when(logrotateBootsrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(logrotateBootsrap).when(underTestSpy).logrotateBootsrap(any(GatewayConfig.class), anyString(), any(Node.class), anyString());

        underTestSpy.startLogrotate(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("rotate", "0.0.1"),
                exitCriteriaModel());
    }

    @Test
    public void consulWatchStartInClusterWhenEverythingWorksFine() throws Exception {
        when(consulWatchBootstrap.call()).thenReturn(true);
        doReturn(consulWatchBootstrap).when(underTestSpy).consulWatchBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), anyString(), any(LogVolumePath.class));

        underTestSpy.startConsulWatches(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("watch", "0.0.1"),
                generateLogVolume(), exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void consulWatchStartInClusterWhenOrchestratorCancelled() throws Exception {
        when(consulWatchBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(consulWatchBootstrap).when(underTestSpy).consulWatchBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), anyString(), any(LogVolumePath.class));

        underTestSpy.startConsulWatches(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("watch", "0.0.1"),
                generateLogVolume(), exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void consulWatchStartInClusterWhenOrchestratorFailed() throws Exception {
        when(consulWatchBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(consulWatchBootstrap).when(underTestSpy).consulWatchBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), anyString(), any(LogVolumePath.class));

        underTestSpy.startConsulWatches(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("watch", "0.0.1"),
                generateLogVolume(), exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void consulWatchStartInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        when(consulWatchBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(consulWatchBootstrap).when(underTestSpy).consulWatchBootstrap(any(GatewayConfig.class), anyString(),
                any(Node.class), anyString(), any(LogVolumePath.class));

        underTestSpy.startConsulWatches(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("watch", "0.0.1"),
                generateLogVolume(), exitCriteriaModel());
    }

    @Test
    public void kerberosServerStartInClusterWhenEverythingWorksFine() throws Exception {
        when(kerberosServerBootstrap.call()).thenReturn(true);
        doReturn(kerberosServerBootstrap).when(underTestSpy).kerberosServerBootstrap(any(KerberosConfiguration.class), any(GatewayConfig.class), anyString(),
                any(Node.class), any(LogVolumePath.class));

        underTestSpy.startKerberosServer(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("bserver", "0.0.1"),
                generateLogVolume(), new KerberosConfiguration("", "", ""), exitCriteriaModel());
    }

    @Test
    public void kerberosServerStartInClusterWhenOrchestratorCancelled() throws Exception {
        when(registratorBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(kerberosServerBootstrap).when(underTestSpy).kerberosServerBootstrap(any(KerberosConfiguration.class), any(GatewayConfig.class), anyString(),
                any(Node.class), any(LogVolumePath.class));

        underTestSpy.startKerberosServer(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("bserver", "0.0.1"),
                generateLogVolume(), new KerberosConfiguration("", "", ""), exitCriteriaModel());
    }

    @Test
    public void kerberosServerStartInClusterWhenOrchestratorFailed() throws Exception {
        when(registratorBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(kerberosServerBootstrap).when(underTestSpy).kerberosServerBootstrap(any(KerberosConfiguration.class), any(GatewayConfig.class), anyString(),
                any(Node.class), any(LogVolumePath.class));

        underTestSpy.startKerberosServer(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("bserver", "0.0.1"),
                generateLogVolume(), new KerberosConfiguration("", "", ""), exitCriteriaModel());
    }

    @Test
    public void kerberosServerStartInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        when(registratorBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(kerberosServerBootstrap).when(underTestSpy).kerberosServerBootstrap(any(KerberosConfiguration.class), any(GatewayConfig.class), anyString(),
                any(Node.class), any(LogVolumePath.class));

        underTestSpy.startKerberosServer(containerOrchestratorCluster(gatewayConfig(), generateNodes(FIX_NODE_COUNT)), new ContainerConfig("bserver", "0.0.1"),
                generateLogVolume(), new KerberosConfiguration("", "", ""), exitCriteriaModel());
    }

}