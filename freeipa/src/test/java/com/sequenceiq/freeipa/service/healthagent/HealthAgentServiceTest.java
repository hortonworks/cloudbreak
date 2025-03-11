package com.sequenceiq.freeipa.service.healthagent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class HealthAgentServiceTest {
    private static final String STOP_HEALTH_AGENT_COMMAND = "systemctl stop cdp-freeipa-healthagent";

    @Mock
    private StackService stackService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @InjectMocks
    private HealthAgentService underTest;

    @Test
    void testStopHealthAgentOnHosts() throws CloudbreakOrchestratorFailedException {
        Long stackId = 1L;
        Set<String> fqdns = Set.of("host1.example.com", "host2.example.com");
        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);

        when(stackService.getStackById(stackId)).thenReturn(stack);
        when(gatewayConfigService.getPrimaryGatewayConfigForSalt(stack)).thenReturn(gatewayConfig);

        underTest.stopHealthAgentOnHosts(stackId, fqdns);

        verify(hostOrchestrator).runCommandOnHosts(eq(List.of(gatewayConfig)), eq(fqdns), eq(STOP_HEALTH_AGENT_COMMAND));
    }

    @Test
    void testStopHealthAgentOnHostsHandlesException() throws CloudbreakOrchestratorFailedException {
        Long stackId = 1L;
        Set<String> fqdns = Set.of("host1.example.com");
        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);

        when(stackService.getStackById(stackId)).thenReturn(stack);
        when(gatewayConfigService.getPrimaryGatewayConfigForSalt(stack)).thenReturn(gatewayConfig);
        doThrow(new CloudbreakOrchestratorFailedException("Error")).when(hostOrchestrator)
                .runCommandOnHosts(eq(List.of(gatewayConfig)), eq(fqdns), eq(STOP_HEALTH_AGENT_COMMAND));

        assertDoesNotThrow(() -> underTest.stopHealthAgentOnHosts(stackId, fqdns));
    }
}