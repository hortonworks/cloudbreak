package com.sequenceiq.cloudbreak.core.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
class ClusterManagerUpgradeServiceTest {

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ClouderaManagerRepo clouderaManagerRepo;

    @InjectMocks
    private ClusterManagerUpgradeService underTest;

    @Spy
    private StackDto stackDto;

    private Stack stack;

    private Cluster cluster;

    @BeforeEach
    public void setUp() {
        stack = TestUtil.stack(Status.AVAILABLE, TestUtil.awsCredential());
        cluster = TestUtil.cluster();
        when(stackDto.hasGateway()).thenReturn(cluster.getGateway() != null);
        when(stackDto.getPrimaryGatewayInstance()).thenReturn(stack.getPrimaryGatewayInstance());
        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getSecurityConfig()).thenReturn(stack.getSecurityConfig());
    }

    @Test
    void testUpgradeClouderaManagerShouldCallTheUpgradeCommand() throws CloudbreakOrchestratorException {
        underTest.upgradeClouderaManager(stackDto, clouderaManagerRepo);

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getSecurityConfig(), stack.getPrimaryGatewayInstance(),
                cluster.getGateway() != null);
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerRepo(any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).createPillarWithClouderaManagerSettings(any(), any(), any());
    }

    @Test
    public void testUpgradeClusterManagerShouldNotAddCsdToPillarWhenTheClusterTypeIsDataLake() throws CloudbreakOrchestratorException {
        stack.setType(StackType.DATALAKE);

        underTest.upgradeClouderaManager(stackDto, clouderaManagerRepo);

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getSecurityConfig(), stack.getPrimaryGatewayInstance(),
                cluster.getGateway() != null);
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerRepo(any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).createPillarWithClouderaManagerSettings(any(), any(), any());
    }

    @Test
    public void testUpgradeClusterManagerShouldAddCsdToPillarWhenTheClusterTypeIsWorkload() throws CloudbreakOrchestratorException {
        stack.setType(StackType.WORKLOAD);

        underTest.upgradeClouderaManager(stackDto, clouderaManagerRepo);

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getSecurityConfig(), stack.getPrimaryGatewayInstance(),
                cluster.getGateway() != null);
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerRepo(any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).createPillarWithClouderaManagerSettings(any(), any(), any());
    }

}