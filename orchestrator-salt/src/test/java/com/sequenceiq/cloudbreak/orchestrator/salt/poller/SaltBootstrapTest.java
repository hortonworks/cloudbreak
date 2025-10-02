package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        GatewayConfig gatewayConfig = new GatewayConfig("1.1.1.1", "10.0.0.1", "172.16.252.43",
                "10-0-0-1.example.com", 9443, "instanceId", "serverCert", "clientCert", "clientKey",
                "saltpasswd", "saltbootpassword", "signkey", false, true, null, null, null, null, null, null, null, null);
        gatewayConfigs = List.of(gatewayConfig);

        GenericResponse response = new GenericResponse();
        response.setStatusCode(HttpStatus.OK.value());
        GenericResponses genericResponses = new GenericResponses();
        genericResponses.setResponses(Collections.singletonList(response));

        when(saltStateService.bootstrap(eq(saltConnector), any(), any(), any())).thenReturn(genericResponses);

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

        BootstrapParams params = new BootstrapParams();
        SaltBootstrap saltBootstrap = spy(new SaltBootstrap(saltStateService, minionUtil, saltConnector, List.of(saltConnector), gatewayConfigs, targets,
                params));
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
                new BootstrapParams()));
        doReturn(mock(MinionAcceptor.class)).when(saltBootstrap).createMinionAcceptor();

        assertThatThrownBy(saltBootstrap::call)
                .hasMessageContaining("10.0.0.3")
                .hasMessageNotContaining("10.0.0.1")
                .hasMessageNotContaining("10.0.0.2")
                .isInstanceOf(CloudbreakOrchestratorFailedException.class);
    }

}
