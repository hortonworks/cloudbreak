package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;

class PillarSaveTest {

    @Test
    void testDiscovery() throws Exception {
        SaltConnector saltConnector = mock(SaltConnector.class);
        GenericResponses responses = new GenericResponses();
        GenericResponse response = new GenericResponse();
        response.setStatusCode(HttpStatus.OK.value());
        response.setAddress("10.0.0.2");
        responses.setResponses(Collections.singletonList(response));
        when(saltConnector.pillar(any(), any(Pillar.class))).thenReturn(responses);

        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node("10.0.0.1", "1.1.1.1", "i-1234", "m5.xlarge", "10-0-0-1.example.com", "hg"));
        nodes.add(new Node("10.0.0.2", "1.1.1.2", "i-1234", "m5.xlarge", "10-0-0-2.example.com", "hg"));
        nodes.add(new Node("10.0.0.3", "1.1.1.3", "i-1234", "m5.xlarge", "10-0-0-3.example.com", "hg"));
        PillarSave pillarSave = PillarSave.createHostsPillar(saltConnector, Sets.newHashSet("10.0.0.1"), nodes);
        pillarSave.call();
        ArgumentCaptor<Pillar> pillarCaptor = ArgumentCaptor.forClass(Pillar.class);
        ArgumentCaptor<Set<String>> targetCaptor = ArgumentCaptor.forClass(Set.class);
        verify(saltConnector).pillar(targetCaptor.capture(), pillarCaptor.capture());
        Pillar pillar = pillarCaptor.getValue();
        Map<String, Map<String, Map<String, Object>>> pillarJson = (Map<String, Map<String, Map<String, Object>>>) pillar.getJson();
        Map<String, Map<String, Object>> hostMap = pillarJson.entrySet().iterator().next().getValue();
        for (Node node : nodes) {
            assertEquals(node.getHostname(), hostMap.get(node.getPrivateIp()).get("fqdn"));
            assertEquals(node.getHostname().split("\\.")[0], hostMap.get(node.getPrivateIp()).get("hostname"));
            assertEquals(Boolean.TRUE, hostMap.get(node.getPrivateIp()).get("public_address"));
        }
    }

}
