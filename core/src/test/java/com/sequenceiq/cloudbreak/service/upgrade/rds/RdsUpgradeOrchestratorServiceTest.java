package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
class RdsUpgradeOrchestratorServiceTest {

    private static final long STACK_ID = 123L;

    @Mock
    private StackService stackService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private Stack stack;

    @Mock
    private Cluster cluster;

    @Captor
    private ArgumentCaptor<OrchestratorStateParams> paramCaptor;

    @InjectMocks
    private RdsUpgradeOrchestratorService underTest;

    @Mock
    private GatewayConfig gatewayConfig;

    private Node node1;

    private Node node2;

    @BeforeEach
    void setUp() {
        node1 = new Node("privateIP1", "publicIP1", "instance1", "instanceType1", "fqdn1", "hostgroup");
        node2 = new Node("privateIP2", "publicIP2", "instance2", "instanceType2", "fqdn2", "hostgroup");
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getCluster()).thenReturn(cluster);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        when(stackUtil.collectGatewayNodes(any())).thenReturn(Set.of(node1, node2));
    }

    @Test
    void testBackupRdsData() throws CloudbreakOrchestratorException {
        underTest.backupRdsData(STACK_ID);
        verify(hostOrchestrator).runOrchestratorState(paramCaptor.capture());
        OrchestratorStateParams params = paramCaptor.getValue();
        assertThat(params.getState()).isEqualTo("postgresql/upgrade/backup");
        assertOtherStateParams(params);
    }

    @Test
    void testRestoreRdsData() throws CloudbreakOrchestratorException {
        underTest.restoreRdsData(STACK_ID);
        verify(hostOrchestrator).runOrchestratorState(paramCaptor.capture());
        OrchestratorStateParams params = paramCaptor.getValue();
        assertThat(params.getState()).isEqualTo("postgresql/upgrade/restore");
        assertOtherStateParams(params);
    }

    private void assertOtherStateParams(OrchestratorStateParams params) {
        assertThat(params.getPrimaryGatewayConfig()).isEqualTo(gatewayConfig);
        assertThat(params.getTargetHostNames()).hasSameElementsAs(Set.of("fqdn1", "fqdn2"));
        assertThat(params.getAllNodes()).hasSameElementsAs(Set.of(node1, node2));
        assertThat(params.getExitCriteriaModel()).isInstanceOf(ClusterDeletionBasedExitCriteriaModel.class);
        assertThat(((ClusterDeletionBasedExitCriteriaModel) params.getExitCriteriaModel()).getStackId().get()).isEqualTo(STACK_ID);
    }

}
