package com.sequenceiq.cloudbreak.core.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

public class ClusterManagerUpgradeServiceTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

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

    @InjectMocks
    private ClusterManagerUpgradeService underTest;

    private Stack stack;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        stack = TestUtil.stack(Status.AVAILABLE, TestUtil.awsCredential());
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
    }

    @Test
    public void testUpgradeClusterManagerWithHostOrchestrator() throws CloudbreakOrchestratorException, CloudbreakException {
        Cluster cluster = stack.getCluster();

        underTest.upgradeClusterManager(STACK_ID);

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getPrimaryGatewayInstance(),  cluster.getGateway() != null);
        verify(clusterComponentConfigProvider, times(1)).getClouderaManagerRepoDetails(cluster.getId());
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());
        verify(clusterApi).stopCluster(true);

    }
}