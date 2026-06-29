package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionIpAddressesResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.join.MinionAcceptor;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.MinionUtil;

@ExtendWith(MockitoExtension.class)
class SaltBootstrapTest {

    @Mock
    private SaltStateService saltStateService;

    @Mock
    private MinionUtil minionUtil;

    @Mock
    private SaltConnector saltConnector;

    private List<GatewayConfig> gatewayConfigs;

    private MinionIpAddressesResponse minionIpAddressesResponse;

    @BeforeEach
    void setUp() {
        GatewayConfig gatewayConfig = GatewayConfig.builder()
                .withConnectionAddress("1.1.1.1")
                .withPublicAddress("10.0.0.1")
                .withPrivateAddress("172.16.252.43")
                .withHostname("10-0-0-1")
                .withGatewayPort(9443)
                .withInstanceId("instanceid")
                .withServerCert("servercert")
                .withClientCert("clientcert")
                .withClientKey("clientkey")
                .withSaltPassword("saltpasswd")
                .withSaltBootPassword("saltbootpassword")
                .withSignatureKey("signkey")
                .withKnoxGatewayEnabled(false)
                .withPrimary(true)
                .withSaltMasterPrivateKey("masterPrivateKey")
                .withSaltMasterPublicKey("masterPublicKey")
                .withSaltSignPrivateKey("privatekey")
                .withSaltSignPublicKey("publickey")
                .build();
        gatewayConfigs = List.of(gatewayConfig);

        GenericResponse response = new GenericResponse();
        response.setStatusCode(HttpStatus.OK.value());
        GenericResponses genericResponses = new GenericResponses();
        genericResponses.setResponses(Collections.singletonList(response));

        when(saltStateService.bootstrap(eq(saltConnector), any(), any(), any())).thenReturn(genericResponses);

        minionIpAddressesResponse = new MinionIpAddressesResponse();
        lenient().when(saltStateService.collectMinionIpAddresses(eq(List.of(saltConnector)))).thenReturn(List.of(minionIpAddressesResponse));
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

        BootstrapParams params = new BootstrapParams();
        SaltBootstrap saltBootstrap = spy(new SaltBootstrap(saltStateService, minionUtil, saltConnector, List.of(saltConnector), gatewayConfigs, targets,
                targets, params));
        doReturn(mock(MinionAcceptor.class)).when(saltBootstrap).createMinionAcceptor();

        saltBootstrap.call();

        verify(saltStateService, times(1)).bootstrap(saltConnector, params, gatewayConfigs, targets);
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

        SaltBootstrap saltBootstrap = spy(new SaltBootstrap(saltStateService, minionUtil, saltConnector, List.of(saltConnector), gatewayConfigs, targets,
                targets, new BootstrapParams()));
        doReturn(mock(MinionAcceptor.class)).when(saltBootstrap).createMinionAcceptor();

        assertThatThrownBy(saltBootstrap::call)
                .hasMessageContaining("10.0.0.3")
                .hasMessageNotContaining("10.0.0.1")
                .hasMessageNotContaining("10.0.0.2")
                .isInstanceOf(CloudbreakOrchestratorFailedException.class);
    }

    @Test
    void callWithMultipleConnectorsTest() throws Exception {
        SaltConnector saltConnector1 = mock(SaltConnector.class);
        SaltConnector saltConnector2 = mock(SaltConnector.class);
        Collection<SaltConnector> saltConnectors = List.of(saltConnector1, saltConnector2);

        // All connectors must return all minions for success
        MinionIpAddressesResponse response1 = new MinionIpAddressesResponse();
        List<Map<String, JsonNode>> result1 = new ArrayList<>();
        Map<String, JsonNode> ipAddressesForMinions1 = new HashMap<>();
        ipAddressesForMinions1.put("10-0-0-1.example.com", JsonUtil.readTree("[\"10.0.0.1\"]"));
        ipAddressesForMinions1.put("10-0-0-2.example.com", JsonUtil.readTree("[\"10.0.0.2\"]"));
        ipAddressesForMinions1.put("10-0-0-3.example.com", JsonUtil.readTree("[\"10.0.0.3\"]"));
        result1.add(ipAddressesForMinions1);
        response1.setResult(result1);

        MinionIpAddressesResponse response2 = new MinionIpAddressesResponse();
        List<Map<String, JsonNode>> result2 = new ArrayList<>();
        Map<String, JsonNode> ipAddressesForMinions2 = new HashMap<>();
        ipAddressesForMinions2.put("10-0-0-1.example.com", JsonUtil.readTree("[\"10.0.0.1\"]"));
        ipAddressesForMinions2.put("10-0-0-2.example.com", JsonUtil.readTree("[\"10.0.0.2\"]"));
        ipAddressesForMinions2.put("10-0-0-3.example.com", JsonUtil.readTree("[\"10.0.0.3\"]"));
        result2.add(ipAddressesForMinions2);
        response2.setResult(result2);

        when(saltStateService.collectMinionIpAddresses(saltConnectors)).thenReturn(List.of(response1, response2));

        Set<Node> targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", null, null, "hg"));
        targets.add(new Node("10.0.0.2", null, null, "hg"));
        targets.add(new Node("10.0.0.3", null, null, "hg"));

        BootstrapParams params = new BootstrapParams();
        SaltBootstrap saltBootstrap = spy(new SaltBootstrap(saltStateService, minionUtil, saltConnector, saltConnectors, gatewayConfigs, targets, targets,
                params));
        doReturn(mock(MinionAcceptor.class)).when(saltBootstrap).createMinionAcceptor();

        saltBootstrap.call();

        verify(saltStateService, times(1)).bootstrap(saltConnector, params, gatewayConfigs, targets);
        verify(saltStateService, times(1)).collectMinionIpAddresses(saltConnectors);
    }

    @Test
    void callWithMultipleConnectorsFailTest() throws IOException {
        SaltConnector saltConnector1 = mock(SaltConnector.class);
        SaltConnector saltConnector2 = mock(SaltConnector.class);
        Collection<SaltConnector> saltConnectors = List.of(saltConnector1, saltConnector2);

        // response1 is missing 10.0.0.2 and 10.0.0.3
        MinionIpAddressesResponse response1 = new MinionIpAddressesResponse();
        List<Map<String, JsonNode>> result1 = new ArrayList<>();
        Map<String, JsonNode> ipAddressesForMinions1 = new HashMap<>();
        ipAddressesForMinions1.put("10-0-0-1.example.com", JsonUtil.readTree("[\"10.0.0.1\"]"));
        result1.add(ipAddressesForMinions1);
        response1.setResult(result1);

        // response2 is missing 10.0.0.1 and 10.0.0.3
        MinionIpAddressesResponse response2 = new MinionIpAddressesResponse();
        List<Map<String, JsonNode>> result2 = new ArrayList<>();
        Map<String, JsonNode> ipAddressesForMinions2 = new HashMap<>();
        ipAddressesForMinions2.put("10-0-0-2.example.com", JsonUtil.readTree("[\"10.0.0.2\"]"));
        result2.add(ipAddressesForMinions2);
        response2.setResult(result2);

        when(saltStateService.collectMinionIpAddresses(saltConnectors)).thenReturn(List.of(response1, response2));

        Set<Node> targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", null, null, "hg"));
        targets.add(new Node("10.0.0.2", null, null, "hg"));
        String missingNodeIp = "10.0.0.3";
        targets.add(new Node(missingNodeIp, null, null, "hg"));

        SaltBootstrap saltBootstrap = spy(new SaltBootstrap(saltStateService, minionUtil, saltConnector, saltConnectors, gatewayConfigs, targets,
                targets, new BootstrapParams()));
        doReturn(mock(MinionAcceptor.class)).when(saltBootstrap).createMinionAcceptor();

        // All three IPs will be considered missing because each was absent from at least one response
        assertThatThrownBy(saltBootstrap::call)
                .hasMessageContaining("10.0.0.1")
                .hasMessageContaining("10.0.0.2")
                .hasMessageContaining("10.0.0.3")
                .isInstanceOf(CloudbreakOrchestratorFailedException.class);

        verify(saltStateService, times(1)).collectMinionIpAddresses(saltConnectors);
    }
}
