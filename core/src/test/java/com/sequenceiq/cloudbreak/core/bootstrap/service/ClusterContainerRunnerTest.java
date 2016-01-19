package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

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
    private ClusterService clusterService;

    @Mock
    private MockContainerOrchestrator mockContainerOrchestrator;

    @Mock
    private CancelledMockContainerOrchestrator cancelledMockContainerOrchestrator;

    @Mock
    private FailedMockContainerOrchestrator failedMockContainerOrchestrator;

    @Mock
    private ContainerConfigService containerConfigService;

    @InjectMocks
    private ClusterContainerRunner underTest;

    @Before
    public void setUp() {
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
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
        ClusterScalingContext context = new ClusterScalingContext(1L, GCP_PLATFORM, hostGroupAdjustmentJson, getPrivateIps(stack),
                new ArrayList<HostMetadata>(), ScalingType.UPSCALE_ONLY_CLUSTER);
        when(containerOrchestratorResolver.get()).thenReturn(new FailedMockContainerOrchestrator());
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));

        underTest.addClusterContainers(context);
    }

    @Test(expected = CancellationException.class)
    public void runNewNodesClusterContainersWhenContainerRunnerCancelled()
            throws CloudbreakException, CloudbreakOrchestratorFailedException, CloudbreakOrchestratorCancelledException {
        Stack stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster(TestUtil.blueprint(), stack, 1L));
        HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
        ClusterScalingContext context = new ClusterScalingContext(1L, GCP_PLATFORM, hostGroupAdjustmentJson, getPrivateIps(stack),
                new ArrayList<HostMetadata>(), ScalingType.UPSCALE_ONLY_CLUSTER);
        when(containerOrchestratorResolver.get()).thenReturn(new CancelledMockContainerOrchestrator());
        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(tlsSecurityService.buildGatewayConfig(anyLong(), anyString(), anyString()))
                .thenReturn(new GatewayConfig("10.0.0.1", "10.0.0.1", "/cert/1"));

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