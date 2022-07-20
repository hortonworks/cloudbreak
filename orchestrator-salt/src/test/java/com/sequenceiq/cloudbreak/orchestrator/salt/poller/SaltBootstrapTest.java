package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionIpAddressesResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.join.MinionAcceptor;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

@ExtendWith(MockitoExtension.class)
class SaltBootstrapTest {

    @Mock
    private SaltStateService saltStateService;

    @Mock
    private SaltConnector saltConnector;

    private GatewayConfig gatewayConfig;

    private MinionIpAddressesResponse minionIpAddressesResponse;

    @BeforeEach
    void setUp() {
        gatewayConfig = new GatewayConfig("1.1.1.1", "10.0.0.1", "172.16.252.43",
                "10-0-0-1.example.com", 9443, "instanceId", "serverCert", "clientCert", "clientKey",
                "saltpasswd", "saltbootpassword", "signkey", false, true, null, null, null, null);

        GenericResponse response = new GenericResponse();
        response.setStatusCode(HttpStatus.OK.value());
        GenericResponses genericResponses = new GenericResponses();
        genericResponses.setResponses(Collections.singletonList(response));

        when(saltConnector.action(any(SaltAction.class))).thenReturn(genericResponses);

        minionIpAddressesResponse = new MinionIpAddressesResponse();
        when(saltStateService.collectMinionIpAddresses(eq(saltConnector))).thenReturn(minionIpAddressesResponse);
    }

    @Test
    void callTest() throws Exception {
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> ipAddressesForMinions = new HashMap<>();
        ipAddressesForMinions.put("10-0-0-1.example.com", JsonUtil.readTree("[\"10.0.0.1\"]"));
        ipAddressesForMinions.put("10-0-0-2.example.com", JsonUtil.readTree("[\"10.0.0.2\"]"));
        ipAddressesForMinions.put("10-0-0-3.example.com", JsonUtil.readTree("[\"10.0.0.3\"]"));
        result.add(ipAddressesForMinions);
        minionIpAddressesResponse.setResult(result);

        Set<Node> targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", null, null, "hg"));
        targets.add(new Node("10.0.0.2", null, null, "hg"));
        targets.add(new Node("10.0.0.3", null, null, "hg"));

        SaltBootstrap saltBootstrap = spy(new SaltBootstrap(saltStateService, saltConnector, List.of(saltConnector),
                Collections.singletonList(gatewayConfig), targets, new BootstrapParams()));
        doReturn(mock(MinionAcceptor.class)).when(saltBootstrap).createMinionAcceptor();

        saltBootstrap.call();
    }

    @Test
    void callFailTest() throws IOException {
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> ipAddressesForMinions = new HashMap<>();
        ipAddressesForMinions.put("10-0-0-1.example.com", JsonUtil.readTree("[\"10.0.0.1\"]"));
        ipAddressesForMinions.put("10-0-0-2.example.com", JsonUtil.readTree("[\"10.0.0.2\"]"));
        result.add(ipAddressesForMinions);
        minionIpAddressesResponse.setResult(result);

        Set<Node> targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", null, null, "hg"));
        targets.add(new Node("10.0.0.2", null, null, "hg"));
        String missingNodeIp = "10.0.0.3";
        targets.add(new Node(missingNodeIp, null, null, "hg"));

        SaltBootstrap saltBootstrap = spy(new SaltBootstrap(saltStateService, saltConnector, List.of(saltConnector),
                Collections.singletonList(gatewayConfig), targets,
                new BootstrapParams()));
        doReturn(mock(MinionAcceptor.class)).when(saltBootstrap).createMinionAcceptor();

        assertThatThrownBy(saltBootstrap::call)
                .hasMessageContaining("10.0.0.3")
                .hasMessageNotContaining("10.0.0.1")
                .hasMessageNotContaining("10.0.0.2")
                .isInstanceOf(CloudbreakOrchestratorFailedException.class);
    }

    @Test
    void restartNeededTrueButFlagNotSupportedBySaltBootstrap() throws Exception {
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> ipAddressesForMinions = new HashMap<>();
        ipAddressesForMinions.put("10-0-0-1.example.com", JsonUtil.readTree("[\"10.0.0.1\"]"));
        ipAddressesForMinions.put("10-0-0-2.example.com", JsonUtil.readTree("[\"10.0.0.2\"]"));
        ipAddressesForMinions.put("10-0-0-3.example.com", JsonUtil.readTree("[\"10.0.0.3\"]"));
        result.add(ipAddressesForMinions);
        minionIpAddressesResponse.setResult(result);

        Set<Node> targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", null, null, "hg"));
        targets.add(new Node("10.0.0.2", null, null, "hg"));
        targets.add(new Node("10.0.0.3", null, null, "hg"));

        BootstrapParams params = new BootstrapParams();
        params.setRestartNeeded(true);
        params.setRestartNeededFlagSupported(false);
        SaltBootstrap saltBootstrap =
                spy(new SaltBootstrap(saltStateService, saltConnector, List.of(saltConnector), Collections.singletonList(gatewayConfig), targets, params));
        doReturn(mock(MinionAcceptor.class)).when(saltBootstrap).createMinionAcceptor();

        saltBootstrap.call();

        ArgumentCaptor<SaltAction> captor = ArgumentCaptor.forClass(SaltAction.class);
        verify(saltConnector, times(1)).action(captor.capture());
        SaltAction saltAction = captor.getValue();
        List<Minion> minions = saltAction.getMinions();
        assertEquals(3, minions.size());
        assertEquals(Collections.singletonList("127.0.0.1"), minions.get(0).getServers());
        assertEquals(Collections.singletonList("127.0.0.1"), minions.get(1).getServers());
        assertEquals(Collections.singletonList("127.0.0.1"), minions.get(2).getServers());
        assertTrue(minions.get(0).isRestartNeeded());
        assertTrue(minions.get(1).isRestartNeeded());
        assertTrue(minions.get(2).isRestartNeeded());
    }

    @Test
    void restartNeededTrueAndFlagSupportedBySaltBootstrap() throws Exception {
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> ipAddressesForMinions = new HashMap<>();
        ipAddressesForMinions.put("10-0-0-1.example.com", JsonUtil.readTree("[\"10.0.0.1\"]"));
        ipAddressesForMinions.put("10-0-0-2.example.com", JsonUtil.readTree("[\"10.0.0.2\"]"));
        ipAddressesForMinions.put("10-0-0-3.example.com", JsonUtil.readTree("[\"10.0.0.3\"]"));
        result.add(ipAddressesForMinions);
        minionIpAddressesResponse.setResult(result);

        Set<Node> targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", null, null, "hg"));
        targets.add(new Node("10.0.0.2", null, null, "hg"));
        targets.add(new Node("10.0.0.3", null, null, "hg"));

        BootstrapParams params = new BootstrapParams();
        params.setRestartNeeded(true);
        params.setRestartNeededFlagSupported(true);
        SaltBootstrap saltBootstrap =
                spy(new SaltBootstrap(saltStateService, saltConnector, List.of(saltConnector), Collections.singletonList(gatewayConfig), targets, params));
        doReturn(mock(MinionAcceptor.class)).when(saltBootstrap).createMinionAcceptor();

        saltBootstrap.call();

        ArgumentCaptor<SaltAction> captor = ArgumentCaptor.forClass(SaltAction.class);
        verify(saltConnector, times(1)).action(captor.capture());
        SaltAction saltAction = captor.getValue();
        List<Minion> minions = saltAction.getMinions();
        assertEquals(3, minions.size());
        assertEquals(Collections.singletonList("172.16.252.43"), minions.get(0).getServers());
        assertEquals(Collections.singletonList("172.16.252.43"), minions.get(1).getServers());
        assertEquals(Collections.singletonList("172.16.252.43"), minions.get(2).getServers());
        assertTrue(minions.get(0).isRestartNeeded());
        assertTrue(minions.get(1).isRestartNeeded());
        assertTrue(minions.get(2).isRestartNeeded());
    }

    @Test
    void restartNeededFalseAndFlagSupportedBySaltBootstrap() throws Exception {
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> ipAddressesForMinions = new HashMap<>();
        ipAddressesForMinions.put("10-0-0-1.example.com", JsonUtil.readTree("[\"10.0.0.1\"]"));
        ipAddressesForMinions.put("10-0-0-2.example.com", JsonUtil.readTree("[\"10.0.0.2\"]"));
        ipAddressesForMinions.put("10-0-0-3.example.com", JsonUtil.readTree("[\"10.0.0.3\"]"));
        result.add(ipAddressesForMinions);
        minionIpAddressesResponse.setResult(result);

        Set<Node> targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", null, null, "hg"));
        targets.add(new Node("10.0.0.2", null, null, "hg"));
        targets.add(new Node("10.0.0.3", null, null, "hg"));

        BootstrapParams params = new BootstrapParams();
        params.setRestartNeeded(false);
        params.setRestartNeededFlagSupported(true);
        SaltBootstrap saltBootstrap =
                spy(new SaltBootstrap(saltStateService, saltConnector, List.of(saltConnector), Collections.singletonList(gatewayConfig), targets, params));
        doReturn(mock(MinionAcceptor.class)).when(saltBootstrap).createMinionAcceptor();

        saltBootstrap.call();

        ArgumentCaptor<SaltAction> captor = ArgumentCaptor.forClass(SaltAction.class);
        verify(saltConnector, times(1)).action(captor.capture());
        SaltAction saltAction = captor.getValue();
        List<Minion> minions = saltAction.getMinions();
        assertEquals(3, minions.size());
        assertEquals(Collections.singletonList("172.16.252.43"), minions.get(0).getServers());
        assertEquals(Collections.singletonList("172.16.252.43"), minions.get(1).getServers());
        assertEquals(Collections.singletonList("172.16.252.43"), minions.get(2).getServers());
        assertFalse(minions.get(0).isRestartNeeded());
        assertFalse(minions.get(1).isRestartNeeded());
        assertFalse(minions.get(2).isRestartNeeded());
    }

    @Test
    void restartNeededFalseAndFlagNotSupportedBySaltBootstrap() throws Exception {
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> ipAddressesForMinions = new HashMap<>();
        ipAddressesForMinions.put("10-0-0-1.example.com", JsonUtil.readTree("[\"10.0.0.1\"]"));
        ipAddressesForMinions.put("10-0-0-2.example.com", JsonUtil.readTree("[\"10.0.0.2\"]"));
        ipAddressesForMinions.put("10-0-0-3.example.com", JsonUtil.readTree("[\"10.0.0.3\"]"));
        result.add(ipAddressesForMinions);
        minionIpAddressesResponse.setResult(result);

        Set<Node> targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", null, null, "hg"));
        targets.add(new Node("10.0.0.2", null, null, "hg"));
        targets.add(new Node("10.0.0.3", null, null, "hg"));

        BootstrapParams params = new BootstrapParams();
        params.setRestartNeeded(false);
        params.setRestartNeededFlagSupported(false);
        SaltBootstrap saltBootstrap =
                spy(new SaltBootstrap(saltStateService, saltConnector, List.of(saltConnector), Collections.singletonList(gatewayConfig), targets, params));
        doReturn(mock(MinionAcceptor.class)).when(saltBootstrap).createMinionAcceptor();

        saltBootstrap.call();

        ArgumentCaptor<SaltAction> captor = ArgumentCaptor.forClass(SaltAction.class);
        verify(saltConnector, times(1)).action(captor.capture());
        SaltAction saltAction = captor.getValue();
        List<Minion> minions = saltAction.getMinions();
        assertEquals(3, minions.size());
        assertEquals(Collections.singletonList("172.16.252.43"), minions.get(0).getServers());
        assertEquals(Collections.singletonList("172.16.252.43"), minions.get(1).getServers());
        assertEquals(Collections.singletonList("172.16.252.43"), minions.get(2).getServers());
        assertFalse(minions.get(0).isRestartNeeded());
        assertFalse(minions.get(1).isRestartNeeded());
        assertFalse(minions.get(2).isRestartNeeded());
    }

}
