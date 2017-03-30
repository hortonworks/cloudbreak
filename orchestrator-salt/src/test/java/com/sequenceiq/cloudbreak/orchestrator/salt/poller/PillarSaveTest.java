package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;

public class PillarSaveTest {

    @Test
    public void testPillarProperties() {
        SaltConnector saltConnector = mock(SaltConnector.class);
        Map<String, Object> pillarJson = new HashMap<>();
        SaltPillarProperties pillarProperties = new SaltPillarProperties("/nodes/hosts.sls", pillarJson);
        new PillarSave(saltConnector, Collections.emptySet(), pillarProperties);
    }

    @Test
    public void testDiscovery() throws Exception {
        SaltConnector saltConnector = mock(SaltConnector.class);
        GenericResponses responses = new GenericResponses();
        GenericResponse response = new GenericResponse();
        response.setStatusCode(HttpStatus.OK.value());
        response.setAddress("10.0.0.2");
        responses.setResponses(Collections.singletonList(response));
        when(saltConnector.pillar(any(), any(Pillar.class))).thenReturn(responses);

        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node("10.0.0.1", "1.1.1.1", "10-0-0-1.example.com"));
        nodes.add(new Node("10.0.0.2", "1.1.1.2", "10-0-0-2.example.com"));
        nodes.add(new Node("10.0.0.3", "1.1.1.3", "10-0-0-3.example.com"));
        PillarSave pillarSave = new PillarSave(saltConnector, Sets.newHashSet("10.0.0.1"), nodes, false);
        pillarSave.call();
        ArgumentCaptor<Pillar> pillarCaptor = ArgumentCaptor.forClass(Pillar.class);
        ArgumentCaptor<Set> targetCaptor = ArgumentCaptor.forClass(Set.class);
        verify(saltConnector).pillar(targetCaptor.capture(), pillarCaptor.capture());
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
        SaltConnector saltConnector = mock(SaltConnector.class);
        GenericResponses responses = new GenericResponses();
        GenericResponse response = new GenericResponse();
        responses.setResponses(Collections.singletonList(response));
        response.setStatusCode(HttpStatus.OK.value());
        when(saltConnector.pillar(any(), any(Pillar.class))).thenReturn(responses);
        PillarSave pillarSave = new PillarSave(saltConnector, Collections.emptySet(), "10.0.0.1");
        Boolean callResult = pillarSave.call();
        Assert.assertTrue(callResult);
    }

    @Test
    public void testCallWithNotFound() {
        SaltConnector saltConnector = mock(SaltConnector.class);
        GenericResponses responses = new GenericResponses();
        GenericResponse response = new GenericResponse();
        response.setStatusCode(HttpStatus.NOT_FOUND.value());
        response.setAddress("10.0.0.1");
        responses.setResponses(Collections.singletonList(response));
        when(saltConnector.pillar(any(), any(Pillar.class))).thenReturn(responses);
        PillarSave pillarSave = new PillarSave(saltConnector, Sets.newHashSet("10.0.0.1"), "10.0.0.1");
        try {
            pillarSave.call();
            Assert.fail("Exception should happen");
        } catch (Exception e) {
            Assert.assertEquals(CloudbreakOrchestratorFailedException.class, e.getClass());
        }
    }
}