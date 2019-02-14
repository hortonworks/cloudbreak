package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ClusterContainerRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerConstraintFactory;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ContainerService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterContainerRunnerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private StackService stackService;

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
        Cluster cluster = TestUtil.cluster(TestUtil.clusterDefinition(), stack, 1L);
        stack.setCluster(cluster);
        HostGroupAdjustmentV4Request hostGroupAdjustment = new HostGroupAdjustmentV4Request();
        hostGroupAdjustment.setHostGroup("agent");
        when(containerOrchestratorResolver.get(anyString())).thenReturn(new FailedMockContainerOrchestrator());
        when(clusterService.retrieveClusterByStackIdWithoutAuth(anyLong())).thenReturn(cluster);
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
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        underTest.addClusterContainers(stack.getId(), hostGroupAdjustment.getHostGroup(), hostGroupAdjustment.getScalingAdjustment());
    }

    @Test(expected = CancellationException.class)
    public void runNewNodesClusterContainersWhenContainerRunnerCancelled()
            throws CloudbreakException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(TestUtil.clusterDefinition(), stack, 1L);
        stack.setCluster(cluster);
        HostGroupAdjustmentV4Request hostGroupAdjustment = new HostGroupAdjustmentV4Request();
        hostGroupAdjustment.setHostGroup("agent");
        when(containerOrchestratorResolver.get(anyString())).thenReturn(new CancelledMockContainerOrchestrator());
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(clusterService.retrieveClusterByStackIdWithoutAuth(anyLong())).thenReturn(cluster);
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
