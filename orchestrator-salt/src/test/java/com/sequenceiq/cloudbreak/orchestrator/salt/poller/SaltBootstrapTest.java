package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionIpAddressesResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.join.MinionAcceptor;

public class SaltBootstrapTest {

    private SaltConnector saltConnector;

    private GatewayConfig gatewayConfig;

    private MinionIpAddressesResponse minionIpAddressesResponse;

    @Before
    public void setUp() {
        saltConnector = mock(SaltConnector.class);
        gatewayConfig = new GatewayConfig("1.1.1.1", "10.0.0.1", "172.16.252.43",
                "10-0-0-1.example.com", 9443, "instanceId", "serverCert", "clientCert", "clientKey",
                "saltpasswd", "saltbootpassword", "signkey", false, true, null, null, null, null);

        GenericResponse response = new GenericResponse();
        response.setStatusCode(HttpStatus.OK.value());
        GenericResponses genericResponses = new GenericResponses();
        genericResponses.setResponses(Collections.singletonList(response));

        when(saltConnector.action(any(SaltAction.class))).thenReturn(genericResponses);

        minionIpAddressesResponse = new MinionIpAddressesResponse();
        when(saltConnector.run(any(), ArgumentMatchers.eq("network.ipaddrs"), any(), any(), anyLong()))
                .thenReturn(minionIpAddressesResponse);
    }

    @Test
    public void callTest() throws Exception {
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

        SaltBootstrap saltBootstrap = new SaltBootstrap(saltConnector, List.of(saltConnector), Collections.singletonList(gatewayConfig), targets,
                new BootstrapParams());
        saltBootstrap = spy(saltBootstrap);
        doReturn(mock(MinionAcceptor.class)).when(saltBootstrap).createMinionAcceptor();

        saltBootstrap.call();
    }

    @Test
    public void callFailTest() throws IOException {
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

        SaltBootstrap saltBootstrap = new SaltBootstrap(saltConnector, List.of(saltConnector), Collections.singletonList(gatewayConfig), targets,
                new BootstrapParams());
        saltBootstrap = spy(saltBootstrap);
        doReturn(mock(MinionAcceptor.class)).when(saltBootstrap).createMinionAcceptor();
        try {
            saltBootstrap.call();
            fail("should throw exception");
        } catch (Exception e) {
            assertEquals(CloudbreakOrchestratorFailedException.class.getSimpleName(), e.getClass().getSimpleName());
            assertThat(e.getMessage(), containsString("10.0.0.3"));
            assertThat(e.getMessage(), not(containsString("10.0.0.2")));
            assertThat(e.getMessage(), not(containsString("10.0.0.1")));
        }
    }

    @Test
    public void restartNeededTrueButFlagNotSupportedBySaltBootstrap() throws Exception {
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
        SaltBootstrap saltBootstrap = new SaltBootstrap(saltConnector, List.of(saltConnector), Collections.singletonList(gatewayConfig), targets, params);
        saltBootstrap = spy(saltBootstrap);
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
    public void restartNeededTrueAndFlagSupportedBySaltBootstrap() throws Exception {
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
        SaltBootstrap saltBootstrap = new SaltBootstrap(saltConnector, List.of(saltConnector), Collections.singletonList(gatewayConfig), targets, params);
        saltBootstrap = spy(saltBootstrap);
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
    public void restartNeededFalseAndFlagSupportedBySaltBootstrap() throws Exception {
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
        SaltBootstrap saltBootstrap = new SaltBootstrap(saltConnector, List.of(saltConnector), Collections.singletonList(gatewayConfig), targets, params);
        saltBootstrap = spy(saltBootstrap);
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
    public void restartNeededFalseAndFlagNotSupportedBySaltBootstrap() throws Exception {
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
        SaltBootstrap saltBootstrap = new SaltBootstrap(saltConnector, List.of(saltConnector), Collections.singletonList(gatewayConfig), targets, params);
        saltBootstrap = spy(saltBootstrap);
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