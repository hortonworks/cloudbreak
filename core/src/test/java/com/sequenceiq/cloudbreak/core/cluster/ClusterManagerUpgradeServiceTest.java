package com.sequenceiq.cloudbreak.core.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.CsdParcelDecorator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@RunWith(MockitoJUnitRunner.class)
public class ClusterManagerUpgradeServiceTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private CsdParcelDecorator csdParcelDecorator;

    @InjectMocks
    private ClusterManagerUpgradeService underTest;

    private Stack stack;

    @Before
    public void setUp() {
        stack = TestUtil.stack(Status.AVAILABLE, TestUtil.awsCredential());
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
    }

    @Test
    public void testUpgradeClusterManager() throws CloudbreakOrchestratorException, CloudbreakException {
        Cluster cluster = stack.getCluster();

        underTest.upgradeClusterManager(STACK_ID, true);

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getPrimaryGatewayInstance(), cluster.getGateway() != null);
        verify(clusterComponentConfigProvider, times(1)).getClouderaManagerRepoDetails(cluster.getId());
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());
        verify(clusterApi).stopCluster(true);
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerRepo(any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerSettings(any(), any(), any());
        verify(clusterApi).startCluster();
    }

    @Test
    public void testUpgradeClusterManagerWithoutStartServices() throws CloudbreakOrchestratorException, CloudbreakException {
        Cluster cluster = stack.getCluster();

        underTest.upgradeClusterManager(STACK_ID, false);

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getPrimaryGatewayInstance(), cluster.getGateway() != null);
        verify(clusterComponentConfigProvider, times(1)).getClouderaManagerRepoDetails(cluster.getId());
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());
        verify(clusterApi).stopCluster(true);
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerRepo(any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerSettings(any(), any(), any());
        verify(clusterApi, never()).startCluster();
    }

    @Test
    public void testUpgradeClusterManagerShouldNotAddCsdToPillarWhenTheClusterTypeIsDataLake() throws CloudbreakOrchestratorException, CloudbreakException {
        Cluster cluster = stack.getCluster();
        stack.setType(StackType.DATALAKE);

        underTest.upgradeClusterManager(STACK_ID, true);

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getPrimaryGatewayInstance(), cluster.getGateway() != null);
        verify(clusterComponentConfigProvider, times(1)).getClouderaManagerRepoDetails(cluster.getId());
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());
        verify(clusterApi).stopCluster(true);
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerRepo(any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerSettings(any(), any(), any());
        verify(clusterApi).startCluster();
        verify(csdParcelDecorator, times(1)).decoratePillarWithCsdParcels(any(), any());
    }

    @Test
    public void testUpgradeClusterManagerShouldAddCsdToPillarWhenTheClusterTypeIsWorkload() throws CloudbreakOrchestratorException, CloudbreakException {
        Cluster cluster = stack.getCluster();
        stack.setType(StackType.WORKLOAD);

        underTest.upgradeClusterManager(STACK_ID, true);

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getPrimaryGatewayInstance(), cluster.getGateway() != null);
        verify(clusterComponentConfigProvider, times(1)).getClouderaManagerRepoDetails(cluster.getId());
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());
        verify(clusterApi).stopCluster(true);
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerRepo(any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerSettings(any(), any(), any());
        verify(clusterApi).startCluster();
        verify(csdParcelDecorator, times(1)).decoratePillarWithCsdParcels(any(), any());
    }
}
