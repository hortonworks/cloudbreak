package com.sequenceiq.cloudbreak.service.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorGrainRunnerParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
public class RdsRecoverySetupServiceTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private PostgresConfigService postgresConfigService = spy(PostgresConfigService.class);

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private GatewayConfig gatewayConfig;

    @InjectMocks
    private RdsRecoverySetupService underTest;

    @Test
    public void testRunRecoverState() throws CloudbreakOrchestratorFailedException {
        ReflectionTestUtils.setField(postgresConfigService, "databasesReusedDuringRecovery", List.of("HIVE"));

        StackView stackView = mock(StackView.class);
        ClusterView clusterView = mock(ClusterView.class);
        SecurityConfig securityConfig = mock(SecurityConfig.class);
        InstanceMetadataView gwInstance = mock(InstanceMetadataView.class);
        StackDto stack = mock(StackDto.class);
        Node node1 = new Node("", "", "", "", "node1fqdn", "");
        Node node2 = new Node("", "", "", "", "node2fqdn", "");

        when(gwInstance.getDiscoveryFQDN()).thenReturn("test-is1-1-1");
        when(stack.getCluster()).thenReturn(clusterView);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(stack.getPrimaryGatewayInstance()).thenReturn(gwInstance);
        when(stack.getStack()).thenReturn(stackView);
        when(gatewayConfigService.getGatewayConfig(any(StackView.class), any(SecurityConfig.class), any(InstanceMetadataView.class), anyBoolean()))
                .thenReturn(gatewayConfig);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        when(stackUtil.collectReachableNodes(stack)).thenReturn(Set.of(node1, node2));
        ArgumentCaptor<OrchestratorGrainRunnerParams> grainRunnerParamsArgumentCaptor = ArgumentCaptor.forClass(OrchestratorGrainRunnerParams.class);

        underTest.addRecoverRole(STACK_ID);

        verify(hostOrchestrator).runOrchestratorGrainRunner(grainRunnerParamsArgumentCaptor.capture());
        OrchestratorGrainRunnerParams grainRunnerParams = grainRunnerParamsArgumentCaptor.getValue();
        assertEquals(Set.of("test-is1-1-1"), grainRunnerParams.getTargetHostNames());
        assertEquals(Set.of(node1, node2), grainRunnerParams.getAllNodes());
        assertEquals(gatewayConfig, grainRunnerParams.getPrimaryGatewayConfig());
        assertEquals("roles", grainRunnerParams.getKey());
        assertEquals("recover", grainRunnerParams.getValue());
    }

    @Test
    public void testRunRecoverStateThrowsException() throws CloudbreakOrchestratorFailedException {
        StackDto stack = mock(StackDto.class);
        InstanceMetadataView gwInstance = mock(InstanceMetadataView.class);
        ClusterView clusterView = mock(ClusterView.class);
        StackView stackView = mock(StackView.class);

        when(stack.getStack()).thenReturn(stackView);
        when(stack.getPrimaryGatewayInstance()).thenReturn(gwInstance);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        when(stack.getCluster()).thenReturn(clusterView);
        doThrow(new CloudbreakOrchestratorFailedException("error")).when(hostOrchestrator).runOrchestratorGrainRunner(any());

        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.addRecoverRole(STACK_ID));
        assertEquals("error", exception.getMessage());
    }

}