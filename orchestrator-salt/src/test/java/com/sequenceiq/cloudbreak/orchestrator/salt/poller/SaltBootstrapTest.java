package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.NetworkInterfaceResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;

public class SaltBootstrapTest {

    private SaltConnector saltConnector;
    private GatewayConfig gatewayConfig;
    private Map<String, String> networkMap;

    @Before
    public void setUp() {
        saltConnector = mock(SaltConnector.class);
        gatewayConfig = new GatewayConfig("1.1.1.1", "10.0.0.1", "10-0-0-1.example.com", 9443, "certDir", "serverCert", "clientCert", "clientKey",
                "saltpasswd", "saltbootpassword", "signkey");

        GenericResponse response = new GenericResponse();
        response.setStatusCode(HttpStatus.OK.value());
        GenericResponses genericResponses = new GenericResponses();
        genericResponses.setResponses(Collections.singletonList(response));

        when(saltConnector.action(Mockito.any(SaltAction.class))).thenReturn(genericResponses);

        NetworkInterfaceResponse networkInterfaceResponse = new NetworkInterfaceResponse();
        List<Map<String, String>> networkResultList = new ArrayList<>();
        networkMap = new HashMap<>();
        networkMap.put("host-10-0-0-1.example.com", "10.0.0.1");
        networkMap.put("host-10-0-0-2.example.com", "10.0.0.2");
        networkMap.put("host-10-0-0-3.example.com", "10.0.0.3");
        networkResultList.add(networkMap);
        networkInterfaceResponse.setResult(networkResultList);
        when(saltConnector.run(Mockito.any(), Mockito.eq("network.interface_ip"), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(networkInterfaceResponse);
    }

    @Test
    public void callTest() {
        Set<Node> targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", null, null));
        targets.add(new Node("10.0.0.2", null, null));
        targets.add(new Node("10.0.0.3", null, null));

        SaltBootstrap saltBootstrap = new SaltBootstrap(saltConnector, gatewayConfig, targets);
        try {
            saltBootstrap.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
//            fail(e.toString());
        }
    }

    @Test
    public void callFailTest() {
        networkMap.clear();
        networkMap.put("host-10-0-0-1.example.com", "10.0.0.1");
        networkMap.put("host-10-0-0-2.example.com", "10.0.0.2");

        Set<Node> targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", null, null));
        targets.add(new Node("10.0.0.2", null, null));
        String missingNodeIp = "10.0.0.3";
        targets.add(new Node(missingNodeIp, null, null));

        SaltBootstrap saltBootstrap = new SaltBootstrap(saltConnector, gatewayConfig, targets);
        try {
            saltBootstrap.call();
            fail("should throw exception");
        } catch (Exception e) {
            assertTrue(CloudbreakOrchestratorFailedException.class.getSimpleName().equals(e.getClass().getSimpleName()));
            assertThat(e.getMessage(), containsString("10.0.0.3"));
            assertThat(e.getMessage(), not(containsString("10.0.0.2")));
            assertThat(e.getMessage(), not(containsString("10.0.0.1")));
        }
    }

}