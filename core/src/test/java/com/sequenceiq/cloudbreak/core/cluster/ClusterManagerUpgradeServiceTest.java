package com.sequenceiq.cloudbreak.core.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

public class ClusterManagerUpgradeServiceTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Mock
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private StackUtil stackUtil;

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
    }

    @Test
    public void testUpgradeClusterManagerWithNotHostOrchestrator() throws CloudbreakException {
        when(orchestratorTypeResolver.resolveType(stack.getOrchestrator().getType())).thenReturn(OrchestratorType.CONTAINER);
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> {
            underTest.upgradeClusterManager(STACK_ID);
        });

        assertEquals("Cloudera Manager upgrade supports host orchestrator only!", exception.getMessage());
    }

    @Test
    public void testUpgradeClusterManagerWithHostOrchestrator() throws CloudbreakOrchestratorException, CloudbreakException {

        Cluster cluster = stack.getCluster();
        HostOrchestrator hostOrchestrator = mock(HostOrchestrator.class);
        when(hostOrchestratorResolver.get(stack.getOrchestrator().getType())).thenReturn(hostOrchestrator);
        when(orchestratorTypeResolver.resolveType(stack.getOrchestrator().getType())).thenReturn(OrchestratorType.HOST);

        underTest.upgradeClusterManager(STACK_ID);

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getPrimaryGatewayInstance(),  cluster.getGateway() != null);
        verify(clusterComponentConfigProvider, times(1)).getClouderaManagerRepoDetails(cluster.getId());
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());

    }
}