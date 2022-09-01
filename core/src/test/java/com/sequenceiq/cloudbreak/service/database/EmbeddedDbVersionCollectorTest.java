package com.sequenceiq.cloudbreak.service.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@ExtendWith(MockitoExtension.class)
class EmbeddedDbVersionCollectorTest {

    private static final String PGW = "pgw";

    private static final String UNKNOWN = "Unknown";

    private static final String DB_VERSION_COMMAND =
            "{ psql -U postgres -c 'show server_version;' -t 2>/dev/null || echo " + UNKNOWN + "; } | { grep -o '[0-9]*\\.' || echo " + UNKNOWN + "; }";

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @InjectMocks
    private EmbeddedDbVersionCollector underTest;

    @Test
    public void testNoPgw() throws CloudbreakOrchestratorFailedException {
        Stack stack = new Stack();
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(List.of(mock(GatewayConfig.class)));

        Optional<String> result = underTest.collectDbVersion(stack);

        assertTrue(result.isEmpty());
        verifyNoInteractions(hostOrchestrator);
    }

    @Test
    public void testUnknownVersion() throws CloudbreakOrchestratorFailedException {
        Stack stack = new Stack();
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.isPrimary()).thenReturn(true);
        when(gatewayConfig.getHostname()).thenReturn(PGW);
        List<GatewayConfig> allGwConfig = List.of(mock(GatewayConfig.class), gatewayConfig);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGwConfig);
        when(hostOrchestrator.runCommandOnHosts(allGwConfig, Set.of(PGW), DB_VERSION_COMMAND)).thenReturn(Map.of(PGW, UNKNOWN));

        Optional<String> result = underTest.collectDbVersion(stack);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testEmptyReturnFromOrchestrator() throws CloudbreakOrchestratorFailedException {
        Stack stack = new Stack();
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.isPrimary()).thenReturn(true);
        when(gatewayConfig.getHostname()).thenReturn(PGW);
        List<GatewayConfig> allGwConfig = List.of(mock(GatewayConfig.class), gatewayConfig);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGwConfig);
        when(hostOrchestrator.runCommandOnHosts(allGwConfig, Set.of(PGW), DB_VERSION_COMMAND)).thenReturn(Map.of());

        Optional<String> result = underTest.collectDbVersion(stack);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testVersionFetched() throws CloudbreakOrchestratorFailedException {
        Stack stack = new Stack();
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.isPrimary()).thenReturn(true);
        when(gatewayConfig.getHostname()).thenReturn(PGW);
        List<GatewayConfig> allGwConfig = List.of(mock(GatewayConfig.class), gatewayConfig);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGwConfig);
        when(hostOrchestrator.runCommandOnHosts(allGwConfig, Set.of(PGW), DB_VERSION_COMMAND)).thenReturn(Map.of(PGW, "10.2"));

        Optional<String> result = underTest.collectDbVersion(stack);

        assertTrue(result.isPresent());
        assertEquals("10", result.get());
    }
}