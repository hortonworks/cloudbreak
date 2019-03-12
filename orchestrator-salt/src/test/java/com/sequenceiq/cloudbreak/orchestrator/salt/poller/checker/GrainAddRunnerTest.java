package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SaltStates.class)
public class GrainAddRunnerTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void submitTest() throws SaltJobFailedException {
        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        Set<Node> allNode = new HashSet<>();
        allNode.add(new Node("10.0.0.1", "5.5.5.1", "10-0-0-1.example.com", "hg"));
        allNode.add(new Node("10.0.0.2", "5.5.5.2", "10-0-0-2.example.com", "hg"));
        allNode.add(new Node("10.0.0.3", "5.5.5.3", "10-0-0-3.example.com", "hg"));

        PowerMockito.mockStatic(SaltStates.class);
        ApplyResponse applyResponse = new ApplyResponse();
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> nodes = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        String[] grains = {"ambari_server"};
        nodes.put("10-0-0-1.example.com", objectMapper.valueToTree(grains));
        nodes.put("10-0-0-2.example.com", objectMapper.valueToTree(grains));
        nodes.put("10-0-0-3.example.com", objectMapper.valueToTree(grains));
        result.add(nodes);
        applyResponse.setResult(result);
        PowerMockito.when(SaltStates.addGrain(any(), any(), anyString(), any())).thenReturn(applyResponse);
        PowerMockito.when(SaltStates.getGrains(any(), any(), any())).thenReturn(nodes);

        GrainAddRunner addRoleChecker = new GrainAddRunner(targets, allNode, "ambari_server");

        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);
        String missingIps = addRoleChecker.submit(saltConnector);
        assertTrue(addRoleChecker.getTarget().isEmpty());
        assertEquals(missingIps, "[]");
    }

    @Test
    public void submitTestWithMissingNode() throws SaltJobFailedException {
        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        Set<Node> allNode = new HashSet<>();
        allNode.add(new Node("10.0.0.1", "5.5.5.1", "10-0-0-1.example.com", "hg"));
        allNode.add(new Node("10.0.0.2", "5.5.5.2", "10-0-0-2.example.com", "hg"));
        allNode.add(new Node("10.0.0.3", "5.5.5.3", "10-0-0-3.example.com", "hg"));

        PowerMockito.mockStatic(SaltStates.class);
        ApplyResponse applyResponse = new ApplyResponse();
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> nodes = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        String[] grains = {"ambari_server"};
        nodes.put("10-0-0-1.example.com", objectMapper.valueToTree(grains));
        nodes.put("10-0-0-2.example.com", objectMapper.valueToTree(grains));
        result.add(nodes);
        applyResponse.setResult(result);
        PowerMockito.when(SaltStates.addGrain(any(), any(), anyString(), any())).thenReturn(applyResponse);
        PowerMockito.when(SaltStates.getGrains(any(), any(), any())).thenReturn(nodes);

        GrainAddRunner addRoleChecker = new GrainAddRunner(targets, allNode, "ambari_server");

        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);
        expectedException.expect(SaltJobFailedException.class);
        expectedException.expectMessage("Can not find node in grains result. target=10-0-0-3.example.com, key=roles, value=ambari_server");
        addRoleChecker.submit(saltConnector);
    }

}