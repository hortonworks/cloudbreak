package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
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
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionIpAddressesResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;

public class SaltBootstrapTest {

    private SaltConnector saltConnector;

    private GatewayConfig gatewayConfig;

    private MinionIpAddressesResponse minionIpAddressesResponse;

    @Before
    public void setUp() {
        saltConnector = mock(SaltConnector.class);
        gatewayConfig = new GatewayConfig("1.1.1.1", "10.0.0.1", "172.16.252.43",
                "10-0-0-1.example.com", 9443, "instanceId", "serverCert", "clientCert", "clientKey",
                "saltpasswd", "saltbootpassword", "signkey", false, true, null, null, null, null, null);

        GenericResponse response = new GenericResponse();
        response.setStatusCode(HttpStatus.OK.value());
        GenericResponses genericResponses = new GenericResponses();
        genericResponses.setResponses(Collections.singletonList(response));

        when(saltConnector.action(ArgumentMatchers.any(SaltAction.class))).thenReturn(genericResponses);

        minionIpAddressesResponse = new MinionIpAddressesResponse();
        when(saltConnector.run(ArgumentMatchers.any(), ArgumentMatchers.eq("network.ipaddrs"), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(minionIpAddressesResponse);
    }

    @Test
    public void callTest() throws IOException {
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

        SaltBootstrap saltBootstrap = new SaltBootstrap(saltConnector, Collections.singletonList(gatewayConfig), targets, new BootstrapParams());
        try {
            saltBootstrap.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

        SaltBootstrap saltBootstrap = new SaltBootstrap(saltConnector, Collections.singletonList(gatewayConfig), targets, new BootstrapParams());
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

}