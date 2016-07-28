package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;

public class PillarSaveTest {

    @Test
    public void testPillarProperties() {
        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);
        Map<String, Object> pillarJson = new HashMap<>();
        SaltPillarProperties pillarProperties = new SaltPillarProperties("/nodes/hosts.sls", pillarJson);
        new PillarSave(saltConnector, pillarProperties);
    }

    @Test
    public void testDiscovery() throws Exception {
        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);
        GenericResponse response = new GenericResponse();
        response.setStatusCode(HttpStatus.OK.value());
        when(saltConnector.pillar(Mockito.any(Pillar.class))).thenReturn(response);

        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node("10.0.0.1", "1.1.1.1", "10-0-0-1.example.com"));
        nodes.add(new Node("10.0.0.2", "1.1.1.2", "10-0-0-2.example.com"));
        nodes.add(new Node("10.0.0.3", "1.1.1.3", "10-0-0-3.example.com"));
        PillarSave pillarSave = new PillarSave(saltConnector, nodes, false);
        pillarSave.call();
        ArgumentCaptor<Pillar> pillarCaptor = ArgumentCaptor.forClass(Pillar.class);
        verify(saltConnector).pillar(pillarCaptor.capture());
        Pillar pillar = pillarCaptor.getValue();
        Map<String, Map<String, Map<String, Object>>> pillarJson = (Map<String, Map<String, Map<String, Object>>>) pillar.getJson();
        Map<String, Map<String, Object>> hostMap = pillarJson.entrySet().iterator().next().getValue();
        for (Node node : nodes) {
            Assert.assertEquals(node.getHostname(), hostMap.get(node.getPrivateIp()).get("fqdn"));
            Assert.assertEquals(node.getHostname().split("\\.")[0], hostMap.get(node.getPrivateIp()).get("hostname"));
            Assert.assertEquals(Boolean.TRUE, hostMap.get(node.getPrivateIp()).get("public_address"));
        }
    }

    @Test
    public void testCall() throws Exception {
        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);
        GenericResponse response = new GenericResponse();
        response.setStatusCode(HttpStatus.OK.value());
        when(saltConnector.pillar(Mockito.any(Pillar.class))).thenReturn(response);
        PillarSave pillarSave = new PillarSave(saltConnector, "10.0.0.1");
        Boolean callResult = pillarSave.call();
        Assert.assertTrue(callResult);
    }

    @Test
    public void testCallWithNotFound() {
        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);
        GenericResponse response = new GenericResponse();
        response.setStatusCode(HttpStatus.NOT_FOUND.value());
        when(saltConnector.pillar(Mockito.any(Pillar.class))).thenReturn(response);
        PillarSave pillarSave = new PillarSave(saltConnector, "10.0.0.1");
        try {
            pillarSave.call();
            Assert.fail("Exception should happen");
        } catch (Exception e) {
            Assert.assertEquals(CloudbreakOrchestratorFailedException.class, e.getClass());
        }
    }
}