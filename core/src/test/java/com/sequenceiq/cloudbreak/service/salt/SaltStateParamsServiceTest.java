package com.sequenceiq.cloudbreak.service.salt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class SaltStateParamsServiceTest {

    private static final Long STACK_ID = 1L;

    private static final Long CLUSTER_ID = 2L;

    private static final String SALT_STATE = "kafka/check-kraft-acls";

    @Mock
    private StackUtil stackUtil;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @InjectMocks
    private SaltStateParamsService underTest;

    @Test
    void testCreateStateParamsForTargetNodesTargetsOnlyTheGivenNodes() {
        StackDto stack = mock(StackDto.class);
        ClusterView cluster = mock(ClusterView.class);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getCluster()).thenReturn(cluster);
        when(cluster.getId()).thenReturn(CLUSTER_ID);
        GatewayConfig primaryGatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(primaryGatewayConfig);
        Set<Node> targetNodes = Set.of(node("broker0.example.com"), node("broker1.example.com"));

        OrchestratorStateParams result = underTest.createStateParamsForTargetNodes(stack, SALT_STATE, targetNodes, 60, 1);

        assertEquals(SALT_STATE, result.getState());
        assertSame(primaryGatewayConfig, result.getPrimaryGatewayConfig());
        assertEquals(Set.of("broker0.example.com", "broker1.example.com"), result.getTargetHostNames());
        OrchestratorStateRetryParams retryParams = result.getStateRetryParams().orElseThrow();
        assertEquals(60, retryParams.getMaxRetry());
        assertEquals(1, retryParams.getMaxRetryOnError());
        assertEquals(-1, retryParams.getSleepTime());
        ClusterDeletionBasedExitCriteriaModel exitCriteriaModel = assertInstanceOf(ClusterDeletionBasedExitCriteriaModel.class, result.getExitCriteriaModel());
        assertEquals(Optional.of(STACK_ID), exitCriteriaModel.getStackId());
        assertEquals(Optional.of(CLUSTER_ID), exitCriteriaModel.getClusterId());
    }

    private Node node(String fqdn) {
        return new Node("10.0.0.1", null, "i-1", "m5.xlarge", fqdn, "broker");
    }
}
