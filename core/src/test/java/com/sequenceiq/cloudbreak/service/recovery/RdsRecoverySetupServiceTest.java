package com.sequenceiq.cloudbreak.service.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorGrainRunnerParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
public class RdsRecoverySetupServiceTest {

    @InjectMocks
    private PostgresConfigService postgresConfigService = Mockito.spy(PostgresConfigService.class);

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private StackService stackService;

    @Mock
    private GatewayConfig gatewayConfig;

    @InjectMocks
    private RdsRecoverySetupService underTest;

    @Test
    public void testRunRecoverState() throws CloudbreakOrchestratorFailedException {
        ReflectionTestUtils.setField(postgresConfigService, "databasesReusedDuringRecovery", List.of("HIVE"));

        Stack stack = TestUtil.stack(TestUtil.cluster());
        Long stackId = stack.getId();
        Node node1 = new Node("", "", "", "", "node1fqdn", "");
        Node node2 = new Node("", "", "", "", "node2fqdn", "");

        when(gatewayConfigService.getGatewayConfig(any(Stack.class), any(InstanceMetaData.class), anyBoolean())).thenReturn(gatewayConfig);
        when(stackService.getByIdWithListsInTransaction(stackId)).thenReturn(stack);
        when(stackUtil.collectReachableNodes(stack)).thenReturn(Set.of(node1, node2));
        ArgumentCaptor<OrchestratorGrainRunnerParams> grainRunnerParamsArgumentCaptor = ArgumentCaptor.forClass(OrchestratorGrainRunnerParams.class);

        underTest.addRecoverRole(stackId);

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
        Stack stack = TestUtil.stack(TestUtil.cluster());
        Long stackId = stack.getId();
        when(stackService.getByIdWithListsInTransaction(stackId)).thenReturn(stack);
        doThrow(new CloudbreakOrchestratorFailedException("error")).when(hostOrchestrator).runOrchestratorGrainRunner(any());

        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.addRecoverRole(stackId));
        assertEquals("error", exception.getMessage());
    }

}