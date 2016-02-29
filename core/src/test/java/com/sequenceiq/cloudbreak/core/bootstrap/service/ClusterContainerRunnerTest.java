package com.sequenceiq.cloudbreak.core.bootstrap.service;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ContainerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusterContainerRunnerTest {

    private static final Platform GCP_PLATFORM = Platform.platform(CloudConstants.GCP);

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

    @Before
    public void setUp() throws CloudbreakException {
        ReflectionTestUtils.setField(containerConfigService, "ambariAgent", "sequence/testcont:0.1.1");
        ReflectionTestUtils.setField(containerConfigService, "ambariServer", "sequence/testcont:0.1.1");
        ReflectionTestUtils.setField(containerConfigService, "registratorDockerImageName", "sequence/testcont:0.1.1");
        ReflectionTestUtils.setField(containerConfigService, "consulWatchPlugnDockerImageName", "sequence/testcont:0.1.1");
        ReflectionTestUtils.setField(containerConfigService, "postgresDockerImageName", "sequence/testcont:0.1.1");
        ReflectionTestUtils.setField(containerConfigService, "kerberosDockerImageName", "sequence/testcont:0.1.1");
        ReflectionTestUtils.setField(containerConfigService, "logrotateDockerImageName", "sequence/testcont:0.1.1");
    }

    @Test(expected = CloudbreakException.class)
    public void runNewNodesClusterContainersWhenContainerRunnerFailed()
            throws CloudbreakException, CloudbreakOrchestratorFailedException, CloudbreakOrchestratorCancelledException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L);
        stack.setCluster(cluster);
        HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
        ClusterScalingContext context = new ClusterScalingContext(1L, GCP_PLATFORM, hostGroupAdjustmentJson, ScalingType.UPSCALE_ONLY_CLUSTER);
        when(containerOrchestratorResolver.get(anyString())).thenReturn(new FailedMockContainerOrchestrator());
        when(clusterService.retrieveClusterByStackId(anyLong())).thenReturn(cluster);

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
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));
        when(instanceMetaDataRepository.findAliveInstancesInInstanceGroup(anyLong())).thenReturn(new ArrayList<InstanceMetaData>());
        when(containerService.save(anyList())).thenReturn(new ArrayList<Container>());
        when(constraintFactory.getAmbariAgentConstraint(ambariServer.getHost(), null, stack.cloudPlatform(),
                TestUtil.hostGroup(), context.getHostGroupAdjustment().getScalingAdjustment())).thenReturn(new ContainerConstraint.Builder().build());
        underTest.addClusterContainers(context);
    }

    @Test(expected = CancellationException.class)
    public void runNewNodesClusterContainersWhenContainerRunnerCancelled()
            throws CloudbreakException, CloudbreakOrchestratorFailedException, CloudbreakOrchestratorCancelledException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L);
        stack.setCluster(cluster);
        HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
        ClusterScalingContext context = new ClusterScalingContext(1L, GCP_PLATFORM, hostGroupAdjustmentJson, ScalingType.UPSCALE_ONLY_CLUSTER);
        when(containerOrchestratorResolver.get(anyString())).thenReturn(new CancelledMockContainerOrchestrator());
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));
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

        underTest.addClusterContainers(context);
    }

    private Set<String> getPrivateIps(Stack stack) {
        Set<String> ips = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            ips.add(instanceMetaData.getPrivateIp());
        }
        return ips;
    }

}