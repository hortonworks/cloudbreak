package com.sequenceiq.freeipa.service.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;

@ExtendWith(MockitoExtension.class)
class FreeIpaSaltPingServiceTest {

    @InjectMocks
    private FreeIpaSaltPingService underTest;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Test
    void testSaltPingWhenPingSucceeded() throws CloudbreakOrchestratorFailedException {
        Stack stack = new Stack();
        GatewayConfig gatewayConfig = new GatewayConfig("connectionAddress", "publicAddress", "privateAddress", 1234, "instanceId", Boolean.FALSE);
        when(gatewayConfigService.getPrimaryGatewayConfigForSalt(stack)).thenReturn(gatewayConfig);
        Node node = new Node("privateIp", "publicIp", "instanceId", "instanceType", "fqdn", "hostgroup");
        when(freeIpaNodeUtilService.mapInstancesToNodes(anySet())).thenReturn(Set.of(node));
        when(hostOrchestrator.ping(eq(Set.of("fqdn")), eq(gatewayConfig))).thenReturn(Map.of("fqdn", Boolean.TRUE));
        assertDoesNotThrow(() -> underTest.saltPing(stack));
    }

    @Test
    void testSaltPingWhenPingFailed() throws CloudbreakOrchestratorFailedException {
        Stack stack = new Stack();
        GatewayConfig gatewayConfig = new GatewayConfig("connectionAddress", "publicAddress", "privateAddress", 1234, "instanceId", Boolean.FALSE);
        when(gatewayConfigService.getPrimaryGatewayConfigForSalt(stack)).thenReturn(gatewayConfig);
        Node node = new Node("privateIp", "publicIp", "instanceId", "instanceType", "fqdn", "hostgroup");
        when(freeIpaNodeUtilService.mapInstancesToNodes(anySet())).thenReturn(Set.of(node));
        when(hostOrchestrator.ping(eq(Set.of("fqdn")), eq(gatewayConfig))).thenReturn(Map.of("fqdn", Boolean.FALSE));
        SaltPingFailedException saltPingFailedException = assertThrows(SaltPingFailedException.class, () -> underTest.saltPing(stack));
        assertThat(saltPingFailedException.getMessage()).startsWith("SaltPing failed");
    }
}