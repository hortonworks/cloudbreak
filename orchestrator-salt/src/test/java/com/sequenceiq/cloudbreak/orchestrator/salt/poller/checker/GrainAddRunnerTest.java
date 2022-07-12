package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

@ExtendWith(MockitoExtension.class)
class GrainAddRunnerTest {

    @Mock
    private SaltStateService saltStateService;

    @Mock
    private SaltConnector saltConnector;

    @Test
    void submitTest() throws SaltJobFailedException {
        Set<String> targets = new HashSet<>();
        targets.add("10-0-0-1.example.com");
        targets.add("10-0-0-2.example.com");
        targets.add("10-0-0-3.example.com");
        Set<Node> allNode = new HashSet<>();
        allNode.add(new Node("10.0.0.1", "5.5.5.1", "i-1234", "m5.xlarge", "10-0-0-1.example.com", "hg"));
        allNode.add(new Node("10.0.0.2", "5.5.5.2", "i-1234", "m5.xlarge", "10-0-0-2.example.com", "hg"));
        allNode.add(new Node("10.0.0.3", "5.5.5.3", "i-1234", "m5.xlarge", "10-0-0-3.example.com", "hg"));

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
        when(saltStateService.addGrain(any(), any(), anyString(), any())).thenReturn(applyResponse);
        when(saltStateService.getGrains(any(), any(), any())).thenReturn(nodes);

        GrainAddRunner addRoleChecker = new GrainAddRunner(saltStateService, targets, allNode, "ambari_server");

        String missingIps = addRoleChecker.submit(saltConnector);
        assertThat(addRoleChecker.getTargetHostnames()).isEmpty();
        assertEquals(missingIps, "[]");
    }

    @Test
    void submitTestWithMissingNode() throws SaltJobFailedException {
        Set<String> targets = new HashSet<>();
        targets.add("10-0-0-1.example.com");
        targets.add("10-0-0-2.example.com");
        targets.add("10-0-0-3.example.com");
        Set<Node> allNode = new HashSet<>();
        allNode.add(new Node("10.0.0.1", "5.5.5.1", "i-1234", "m5.xlarge", "10-0-0-1.example.com", "hg"));
        allNode.add(new Node("10.0.0.2", "5.5.5.2", "i-1234", "m5.xlarge", "10-0-0-2.example.com", "hg"));
        allNode.add(new Node("10.0.0.3", "5.5.5.3", "i-1234", "m5.xlarge", "10-0-0-3.example.com", "hg"));

        ApplyResponse applyResponse = new ApplyResponse();
        List<Map<String, JsonNode>> result = new ArrayList<>();
        Map<String, JsonNode> nodes = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        String[] grains = {"ambari_server"};
        nodes.put("10-0-0-1.example.com", objectMapper.valueToTree(grains));
        nodes.put("10-0-0-2.example.com", objectMapper.valueToTree(grains));
        result.add(nodes);
        applyResponse.setResult(result);
        when(saltStateService.addGrain(any(), any(), anyString(), any())).thenReturn(applyResponse);
        when(saltStateService.getGrains(any(), any(), any())).thenReturn(nodes);

        GrainAddRunner addRoleChecker = new GrainAddRunner(saltStateService, targets, allNode, "ambari_server");

        assertThatThrownBy(() -> addRoleChecker.submit(saltConnector))
                .hasMessage("Can not find node in grains result. target=10-0-0-3.example.com, key=roles, value=ambari_server")
                .isInstanceOf(SaltJobFailedException.class);
    }

}
