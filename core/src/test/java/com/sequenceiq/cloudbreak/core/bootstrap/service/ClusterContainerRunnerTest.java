package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ClusterContainerRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerConstraintFactory;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint.Builder;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ContainerService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterContainerRunnerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private StackRepository stackRepository;

    @Mock
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private ContainerService containerService;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private ClusterService clusterService;

    @Mock
    private HostGroupRepository hostGroupRepository;

    @Mock
    private MockContainerOrchestrator mockContainerOrchestrator;

    @Mock
    private CancelledMockContainerOrchestrator cancelledMockContainerOrchestrator;

    @Mock
    private FailedMockContainerOrchestrator failedMockContainerOrchestrator;

    @Mock
    private ContainerConfigService containerConfigService;

    @Mock
    private ContainerConstraintFactory constraintFactory;

    @InjectMocks
    private ClusterContainerRunner underTest;

    @Test
    public void runNewNodesClusterContainersWhenContainerRunnerFailed() throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L);
        stack.setCluster(cluster);
        HostGroupAdjustmentJson hostGroupAdjustment = new HostGroupAdjustmentJson();
        hostGroupAdjustment.setHostGroup("agent");
        when(containerOrchestratorResolver.get(anyString())).thenReturn(new FailedMockContainerOrchestrator());
        when(clusterService.retrieveClusterByStackId(anyLong())).thenReturn(cluster);
        thrown.expect(CloudbreakException.class);
        thrown.expectMessage("com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException: failed");

        Set<Container> containers = new HashSet<>();

        Container ambariServer = new Container();
        ambariServer.setName("server");
        ambariServer.setImage(DockerContainer.AMBARI_SERVER.getName());
        ambariServer.setHost("hostname-1");
        ambariServer.setContainerId("1");

        Container ambariAgent = new Container();
        ambariAgent.setName("agent");
        ambariAgent.setImage(DockerContainer.AMBARI_AGENT.getName());
        ambariAgent.setHost("hostname-2");
        ambariAgent.setContainerId("1");

        containers.add(ambariAgent);
        containers.add(ambariServer);

        when(containerService.findContainersInCluster(anyLong())).thenReturn(containers);
        when(hostGroupRepository.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(TestUtil.hostGroup());
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), any(InstanceMetaData.class), anyInt(), any(), any()))
                .thenReturn(new GatewayConfig("10.0.0.1", "198.0.0.1", "10.0.0.1", 8443, false));
        when(instanceMetaDataRepository.findAliveInstancesInInstanceGroup(anyLong())).thenReturn(new ArrayList<>());
        when(containerService.save(anyList())).thenReturn(new ArrayList<>());
        when(constraintFactory.getAmbariAgentConstraint(ambariServer.getHost(), null, stack.cloudPlatform(),
                TestUtil.hostGroup(), hostGroupAdjustment.getScalingAdjustment(), new ArrayList<>(), ""))
                .thenReturn(new Builder().build());
        underTest.addClusterContainers(stack.getId(), hostGroupAdjustment.getHostGroup(), hostGroupAdjustment.getScalingAdjustment());
    }

    @Test(expected = CancellationException.class)
    public void runNewNodesClusterContainersWhenContainerRunnerCancelled()
            throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L);
        stack.setCluster(cluster);
        HostGroupAdjustmentJson hostGroupAdjustment = new HostGroupAdjustmentJson();
        hostGroupAdjustment.setHostGroup("agent");
        when(containerOrchestratorResolver.get(anyString())).thenReturn(new CancelledMockContainerOrchestrator());
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), any(InstanceMetaData.class), anyInt(), any(), any()))
                .thenReturn(new GatewayConfig("10.0.0.1", "198.0.0.1", "10.0.0.1", 8443, false));
        when(clusterService.retrieveClusterByStackId(anyLong())).thenReturn(cluster);
        when(hostGroupRepository.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(TestUtil.hostGroup());

        Set<Container> containers = new HashSet<>();

        Container ambariServer = new Container();
        ambariServer.setName("server");
        ambariServer.setImage(DockerContainer.AMBARI_SERVER.getName());
        ambariServer.setHost("hostname-1");
        ambariServer.setContainerId("1");

        Container ambariAgent = new Container();
        ambariAgent.setName("agent");
        ambariAgent.setImage(DockerContainer.AMBARI_AGENT.getName());
        ambariAgent.setHost("hostname-2");
        ambariAgent.setContainerId("1");

        containers.add(ambariAgent);
        containers.add(ambariServer);

        when(containerService.findContainersInCluster(anyLong())).thenReturn(containers);

        underTest.addClusterContainers(stack.getId(), hostGroupAdjustment.getHostGroup(), hostGroupAdjustment.getScalingAdjustment());
    }
}
