package com.sequenceiq.cloudbreak.orchestrator.swarm;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.AmbariAgentBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.AmbariServerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.AmbariServerDatabaseBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.BaywatchClientBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.BaywatchServerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.ConsulWatchBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.LogrotateBootsrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.MunchausenBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.RegistratorBootstrap;

@RunWith(MockitoJUnitRunner.class)
public class SwarmContainerOrchestratorTest {

    private SwarmContainerOrchestrator underTest = new SwarmContainerOrchestrator();

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
    private BaywatchClientBootstrap baywatchClientBootstrap;

    @Mock
    private BaywatchServerBootstrap baywatchServerBootstrap;

    @Mock
    private ConsulWatchBootstrap consulWatchBootstrap;

    @Mock
    private LogrotateBootsrap logrotateBootsrap;

    @Test
    public void bootstrapClusterWhenEverythingWorksFine() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(munchausenBootstrap.call()).thenReturn(true);
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenBootstrap(any(GatewayConfig.class), any(String[].class));

        underTestSpy.bootstrap(OrchestratorTestUtil.gatewayConfig(), OrchestratorTestUtil.generateNodes(10), 3, OrchestratorTestUtil.exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void bootstrapClusterWhenOrchestratorCancelled() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(munchausenBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenBootstrap(any(GatewayConfig.class), any(String[].class));

        underTestSpy.bootstrap(OrchestratorTestUtil.gatewayConfig(), OrchestratorTestUtil.generateNodes(10), 3, OrchestratorTestUtil.exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void bootstrapClusterWhenOrchestratorFailed() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(munchausenBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenBootstrap(any(GatewayConfig.class), any(String[].class));

        underTestSpy.bootstrap(OrchestratorTestUtil.gatewayConfig(), OrchestratorTestUtil.generateNodes(10), 3, OrchestratorTestUtil.exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void bootstrapClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(munchausenBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenBootstrap(any(GatewayConfig.class), any(String[].class));

        underTestSpy.bootstrap(OrchestratorTestUtil.gatewayConfig(), OrchestratorTestUtil.generateNodes(10), 3, OrchestratorTestUtil.exitCriteriaModel());
    }

    @Test
    public void bootstrapNewNodesInClusterWhenEverythingWorksFine() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(munchausenBootstrap.call()).thenReturn(true);
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenNewNodeBootstrap(any(GatewayConfig.class), any(String[].class));

        underTestSpy.bootstrapNewNodes(OrchestratorTestUtil.gatewayConfig(), OrchestratorTestUtil.generateNodes(10), OrchestratorTestUtil.exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void bootstrapNewNodesInClusterWhenOrchestratorCancelled() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(munchausenBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenNewNodeBootstrap(any(GatewayConfig.class), any(String[].class));

        underTestSpy.bootstrapNewNodes(OrchestratorTestUtil.gatewayConfig(), OrchestratorTestUtil.generateNodes(10), OrchestratorTestUtil.exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void bootstrapNewNodesInClusterWhenOrchestratorFailed() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(munchausenBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenNewNodeBootstrap(any(GatewayConfig.class), any(String[].class));

        underTestSpy.bootstrapNewNodes(OrchestratorTestUtil.gatewayConfig(), OrchestratorTestUtil.generateNodes(10), OrchestratorTestUtil.exitCriteriaModel());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void bootstrapNewNodesInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(munchausenBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(munchausenBootstrap).when(underTestSpy).munchausenNewNodeBootstrap(any(GatewayConfig.class), any(String[].class));

        underTestSpy.bootstrapNewNodes(OrchestratorTestUtil.gatewayConfig(), OrchestratorTestUtil.generateNodes(10), OrchestratorTestUtil.exitCriteriaModel());
    }

    @Test
    public void registratorStartInClusterWhenEverythingWorksFine() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(registratorBootstrap.call()).thenReturn(true);
        doReturn(registratorBootstrap).when(underTestSpy).startRegistrator(any(ContainerOrchestratorCluster.class), anyString(), any(ExitCriteriaModel.class));

        underTestSpy.registratorBootstrap(OrchestratorTestUtil.gatewayConfig(), "registrator", OrchestratorTestUtil.node(Long.valueOf(1)));
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void registratorStartInClusterWhenOrchestratorCancelled() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(registratorBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(registratorBootstrap).when(underTestSpy).startRegistrator(any(ContainerOrchestratorCluster.class), anyString(), any(ExitCriteriaModel.class));

        underTestSpy.registratorBootstrap(OrchestratorTestUtil.gatewayConfig(), "registrator", OrchestratorTestUtil.node(Long.valueOf(1)));
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void registratorStartInClusterWhenOrchestratorFailed() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(registratorBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(registratorBootstrap).when(underTestSpy).startRegistrator(any(ContainerOrchestratorCluster.class), anyString(), any(ExitCriteriaModel.class));

        underTestSpy.registratorBootstrap(OrchestratorTestUtil.gatewayConfig(), "registrator", OrchestratorTestUtil.node(Long.valueOf(1)));
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void registratorStartInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(registratorBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(registratorBootstrap).when(underTestSpy).startRegistrator(any(ContainerOrchestratorCluster.class), anyString(), any(ExitCriteriaModel.class));

        underTestSpy.registratorBootstrap(OrchestratorTestUtil.gatewayConfig(), "registrator", OrchestratorTestUtil.node(Long.valueOf(1)));
    }

    @Test
    public void ambariServerStartInClusterWhenEverythingWorksFine() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(ambariServerBootstrap.call()).thenReturn(true);
        doReturn(ambariServerBootstrap).when(underTestSpy).ambariServerBootstrap(any(GatewayConfig.class), anyString(), any(Node.class), anyString());
        when(ambariServerDatabaseBootstrap.call()).thenReturn(true);
        doReturn(ambariServerDatabaseBootstrap).when(underTestSpy).ambariServerDatabaseBootstrap(any(GatewayConfig.class), anyString(), any(Node.class));

        underTestSpy.ambariServerBootstrap(OrchestratorTestUtil.gatewayConfig(), "server", OrchestratorTestUtil.node(Long.valueOf(1)), "azure");
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void ambariServerStartInClusterWhenOrchestratorCancelled() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(ambariServerBootstrap.call()).thenReturn(true);
        doReturn(ambariServerBootstrap).when(underTestSpy).ambariServerBootstrap(any(GatewayConfig.class), anyString(), any(Node.class), anyString());
        when(ambariServerDatabaseBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(ambariServerDatabaseBootstrap).when(underTestSpy).ambariServerDatabaseBootstrap(any(GatewayConfig.class), anyString(), any(Node.class));

        underTestSpy.ambariServerBootstrap(OrchestratorTestUtil.gatewayConfig(), "server", OrchestratorTestUtil.node(Long.valueOf(1)), "azure");
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void ambariServerStartInClusterWhenOrchestratorFailed() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(ambariServerBootstrap.call()).thenReturn(true);
        doReturn(ambariServerBootstrap).when(underTestSpy).ambariServerBootstrap(any(GatewayConfig.class), anyString(), any(Node.class), anyString());
        when(ambariServerDatabaseBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("cancelled"));
        doReturn(ambariServerDatabaseBootstrap).when(underTestSpy).ambariServerDatabaseBootstrap(any(GatewayConfig.class), anyString(), any(Node.class));

        underTestSpy.ambariServerBootstrap(OrchestratorTestUtil.gatewayConfig(), "server", OrchestratorTestUtil.node(Long.valueOf(1)), "azure");
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void ambariServerStartInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(ambariServerBootstrap.call()).thenReturn(true);
        doReturn(ambariServerBootstrap).when(underTestSpy).ambariServerBootstrap(any(GatewayConfig.class), anyString(), any(Node.class), anyString());
        when(ambariServerDatabaseBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(ambariServerDatabaseBootstrap).when(underTestSpy).ambariServerDatabaseBootstrap(any(GatewayConfig.class), anyString(), any(Node.class));

        underTestSpy.ambariServerBootstrap(OrchestratorTestUtil.gatewayConfig(), "server", OrchestratorTestUtil.node(Long.valueOf(1)), "azure");
    }

    @Test
    public void ambariAgentStartInClusterWhenEverythingWorksFine() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(ambariAgentBootstrap.call()).thenReturn(true);
        doReturn(ambariAgentBootstrap).when(underTestSpy).startAmbariAgents(any(ContainerOrchestratorCluster.class), anyString(),
                anyInt(), anyString(), any(ExitCriteriaModel.class));

        underTestSpy.ambariAgentBootstrap(OrchestratorTestUtil.gatewayConfig(), "agent", OrchestratorTestUtil.node(Long.valueOf(1)), "time", "azure");
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void ambariAgentStartInClusterWhenOrchestratorCancelled() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(ambariAgentBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(ambariAgentBootstrap).when(underTestSpy).startAmbariAgents(any(ContainerOrchestratorCluster.class), anyString(),
                anyInt(), anyString(), any(ExitCriteriaModel.class));

        underTestSpy.ambariAgentBootstrap(OrchestratorTestUtil.gatewayConfig(), "agent", OrchestratorTestUtil.node(Long.valueOf(1)), "time", "azure");
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void ambariAgentStartInClusterWhenOrchestratorFailed() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(ambariAgentBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(ambariAgentBootstrap).when(underTestSpy).startAmbariAgents(any(ContainerOrchestratorCluster.class), anyString(),
                anyInt(), anyString(), any(ExitCriteriaModel.class));

        underTestSpy.ambariAgentBootstrap(OrchestratorTestUtil.gatewayConfig(), "agent", OrchestratorTestUtil.node(Long.valueOf(1)), "time", "azure");
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void ambariAgentStartInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(ambariAgentBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(ambariAgentBootstrap).when(underTestSpy).startAmbariAgents(any(ContainerOrchestratorCluster.class), anyString(),
                anyInt(), anyString(), any(ExitCriteriaModel.class));

        underTestSpy.ambariAgentBootstrap(OrchestratorTestUtil.gatewayConfig(), "agent", OrchestratorTestUtil.node(Long.valueOf(1)), "time", "azure");
    }

    @Test
    public void logRotateStartInClusterWhenEverythingWorksFine() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(logrotateBootsrap.call()).thenReturn(true);
        doReturn(logrotateBootsrap).when(underTestSpy).startLogrotate(any(ContainerOrchestratorCluster.class), anyString(),
                anyInt(), any(ExitCriteriaModel.class));

        underTestSpy.logrotateBootsrap(OrchestratorTestUtil.gatewayConfig(), "rotate", "time");
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void logRotateStartInClusterWhenOrchestratorCancelled() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(logrotateBootsrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(logrotateBootsrap).when(underTestSpy).startLogrotate(any(ContainerOrchestratorCluster.class), anyString(),
                anyInt(), any(ExitCriteriaModel.class));

        underTestSpy.logrotateBootsrap(OrchestratorTestUtil.gatewayConfig(), "rotate", "time");
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void logRotateStartInClusterWhenOrchestratorFailed() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(logrotateBootsrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(logrotateBootsrap).when(underTestSpy).startLogrotate(any(ContainerOrchestratorCluster.class), anyString(),
                anyInt(), any(ExitCriteriaModel.class));

        underTestSpy.logrotateBootsrap(OrchestratorTestUtil.gatewayConfig(), "rotate", "time");
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void logRotateStartInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(logrotateBootsrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(logrotateBootsrap).when(underTestSpy).startLogrotate(any(ContainerOrchestratorCluster.class), anyString(),
                anyInt(), any(ExitCriteriaModel.class));

        underTestSpy.logrotateBootsrap(OrchestratorTestUtil.gatewayConfig(), "rotate", "time");
    }

    @Test
    public void consulWatchStartInClusterWhenEverythingWorksFine() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(consulWatchBootstrap.call()).thenReturn(true);
        doReturn(consulWatchBootstrap).when(underTestSpy).startConsulWatches(any(ContainerOrchestratorCluster.class), anyString(),
                anyInt(), any(ExitCriteriaModel.class));

        underTestSpy.consulWatchBootstrap(OrchestratorTestUtil.gatewayConfig(), "watch", "time");
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void consulWatchStartInClusterWhenOrchestratorCancelled() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(consulWatchBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(consulWatchBootstrap).when(underTestSpy).startConsulWatches(any(ContainerOrchestratorCluster.class), anyString(),
                anyInt(), any(ExitCriteriaModel.class));

        underTestSpy.consulWatchBootstrap(OrchestratorTestUtil.gatewayConfig(), "watch", "time");
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void consulWatchStartInClusterWhenOrchestratorFailed() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(consulWatchBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(consulWatchBootstrap).when(underTestSpy).startConsulWatches(any(ContainerOrchestratorCluster.class), anyString(),
                anyInt(), any(ExitCriteriaModel.class));

        underTestSpy.consulWatchBootstrap(OrchestratorTestUtil.gatewayConfig(), "watch", "time");
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void consulWatchStartInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(consulWatchBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(consulWatchBootstrap).when(underTestSpy).startConsulWatches(any(ContainerOrchestratorCluster.class), anyString(),
                anyInt(), any(ExitCriteriaModel.class));

        underTestSpy.consulWatchBootstrap(OrchestratorTestUtil.gatewayConfig(), "watch", "time");
    }

    @Test
    public void baywatchServerStartInClusterWhenEverythingWorksFine() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(baywatchServerBootstrap.call()).thenReturn(true);
        doReturn(baywatchServerBootstrap).when(underTestSpy).startBaywatchServer(any(ContainerOrchestratorCluster.class), anyString(),
                any(ExitCriteriaModel.class));

        underTestSpy.baywatchServerBootstrap(OrchestratorTestUtil.gatewayConfig(), "bserver", OrchestratorTestUtil.node(Long.valueOf(0)));
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void baywatchServerStartInClusterWhenOrchestratorCancelled() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(baywatchServerBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(baywatchServerBootstrap).when(underTestSpy).startBaywatchServer(any(ContainerOrchestratorCluster.class), anyString(),
                any(ExitCriteriaModel.class));

        underTestSpy.baywatchServerBootstrap(OrchestratorTestUtil.gatewayConfig(), "bserver", OrchestratorTestUtil.node(Long.valueOf(0)));
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void baywatchServerStartInClusterWhenOrchestratorFailed() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(baywatchServerBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(baywatchServerBootstrap).when(underTestSpy).startBaywatchServer(any(ContainerOrchestratorCluster.class), anyString(),
                any(ExitCriteriaModel.class));

        underTestSpy.baywatchServerBootstrap(OrchestratorTestUtil.gatewayConfig(), "bserver", OrchestratorTestUtil.node(Long.valueOf(0)));
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void baywatchServerStartInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(baywatchServerBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(baywatchServerBootstrap).when(underTestSpy).startBaywatchServer(any(ContainerOrchestratorCluster.class), anyString(),
                any(ExitCriteriaModel.class));

        underTestSpy.baywatchServerBootstrap(OrchestratorTestUtil.gatewayConfig(), "bserver", OrchestratorTestUtil.node(Long.valueOf(0)));
    }

    @Test
    public void baywatchClientStartInClusterWhenEverythingWorksFine() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(baywatchClientBootstrap.call()).thenReturn(true);
        doReturn(baywatchClientBootstrap).when(underTestSpy).startBaywatchClients(any(ContainerOrchestratorCluster.class), anyString(),
                anyString(), anyInt(), anyString(), anyString(), any(ExitCriteriaModel.class));

        underTestSpy.baywatchClientBootstrap(OrchestratorTestUtil.gatewayConfig(), "10.0.0.1", "bclient", "time", OrchestratorTestUtil.node(Long.valueOf(0)),
                "consul", "external");
    }

    @Test(expected = CloudbreakOrchestratorCancelledException.class)
    public void baywatchClientStartInClusterWhenOrchestratorCancelled() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(baywatchClientBootstrap.call()).thenThrow(new CloudbreakOrchestratorCancelledException("cancelled"));
        doReturn(baywatchClientBootstrap).when(underTestSpy).startBaywatchClients(any(ContainerOrchestratorCluster.class), anyString(),
                anyString(), anyInt(), anyString(), anyString(), any(ExitCriteriaModel.class));

        underTestSpy.baywatchClientBootstrap(OrchestratorTestUtil.gatewayConfig(), "10.0.0.1", "bclient", "time", OrchestratorTestUtil.node(Long.valueOf(0)),
                "consul", "external");
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void baywatchClientStartInClusterWhenOrchestratorFailed() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(baywatchClientBootstrap.call()).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        doReturn(baywatchClientBootstrap).when(underTestSpy).startBaywatchClients(any(ContainerOrchestratorCluster.class), anyString(),
                anyString(), anyInt(), anyString(), anyString(), any(ExitCriteriaModel.class));

        underTestSpy.baywatchClientBootstrap(OrchestratorTestUtil.gatewayConfig(), "10.0.0.1", "bclient", "time", OrchestratorTestUtil.node(Long.valueOf(0)),
                "consul", "external");
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void baywatchClientStartInClusterWhenNullPointerOccurredAndOrchestratorFailedComes() throws Exception {
        SwarmContainerOrchestrator underTestSpy = spy(underTest);
        when(baywatchClientBootstrap.call()).thenThrow(new NullPointerException("null"));
        doReturn(baywatchClientBootstrap).when(underTestSpy).startBaywatchClients(any(ContainerOrchestratorCluster.class), anyString(),
                anyString(), anyInt(), anyString(), anyString(), any(ExitCriteriaModel.class));

        underTestSpy.baywatchClientBootstrap(OrchestratorTestUtil.gatewayConfig(), "10.0.0.1", "bclient", "time", OrchestratorTestUtil.node(Long.valueOf(0)),
                "consul", "external");
    }


}