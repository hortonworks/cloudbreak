package com.sequenceiq.cloudbreak.service.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class OrchestratorStateParamsProviderTest {

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 2L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @InjectMocks
    private OrchestratorStateParamsProvider underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView cluster;

    @Mock
    private GatewayConfig primaryGatewayConfig;

    @BeforeEach
    void setUp() {
        lenient().when(stackDto.getId()).thenReturn(STACK_ID);
        lenient().when(stackDto.getCluster()).thenReturn(cluster);
        lenient().when(cluster.getId()).thenReturn(CLUSTER_ID);
        lenient().when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        lenient().when(gatewayConfigService.getPrimaryGatewayConfig(stackDto)).thenReturn(primaryGatewayConfig);
        Node n1 = mock(Node.class);
        lenient().when(n1.getHostname()).thenReturn("h1");
        Node n2 = mock(Node.class);
        lenient().when(n2.getHostname()).thenReturn("h2");
        lenient().when(stackUtil.collectGatewayNodes(stackDto)).thenReturn(Set.of(n1, n2));
    }

    @Test
    void createStateParams() {
        String state = "state";

        OrchestratorStateParams result = underTest.createStateParams(STACK_ID, state);

        assertThat(result)
                .returns(state, OrchestratorStateParams::getState)
                .returns(primaryGatewayConfig, OrchestratorStateParams::getPrimaryGatewayConfig)
                .returns(Set.of("h1", "h2"), OrchestratorStateParams::getTargetHostNames)

                .extracting(OrchestratorStateParams::getExitCriteriaModel)
                .isInstanceOf(ClusterDeletionBasedExitCriteriaModel.class)
                .extracting(ClusterDeletionBasedExitCriteriaModel.class::cast)
                .returns(Optional.of(STACK_ID), ClusterDeletionBasedExitCriteriaModel::getStackId)
                .returns(Optional.of(CLUSTER_ID), ClusterDeletionBasedExitCriteriaModel::getClusterId);
    }

}
