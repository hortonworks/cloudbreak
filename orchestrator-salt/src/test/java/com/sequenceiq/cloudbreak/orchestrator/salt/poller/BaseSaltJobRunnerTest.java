package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

@ExtendWith(MockitoExtension.class)
class BaseSaltJobRunnerTest {

    private Set<String> targetHostnames = new HashSet<>();

    @Mock
    private SaltStateService saltStateService;

    private BaseSaltJobRunner baseSaltJobRunner;

    @BeforeEach
    void setUp() {
        targetHostnames = new HashSet<>();
    }

    @Test
    void collectMissingNodesWhenAllNodeAndSaltNodeWithPostfix() {
        setupTargetWithPostfix();
        Set<Node> allNode = allNodeWithPostFix();
        baseSaltJobRunner = new BaseSaltJobRunner(saltStateService, targetHostnames, allNode) {
            @Override
            public String submit(SaltConnector saltConnector) {
                return "";
            }
        };

        Set<String> returnedNodes = new HashSet<>();
        returnedNodes.add("host-10-0-0-1.example.com");
        returnedNodes.add("host-10-0-0-2.example.com");
        Set<String> collectedMissingNodes = baseSaltJobRunner.collectMissingHostnames(returnedNodes);

        assertEquals(1L, collectedMissingNodes.size());
        assertEquals("host-10-0-0-3.example.com", collectedMissingNodes.iterator().next());
    }

    @Test
    void collectMissingNodesWhenAllNodeWithoutPostfixSaltNodeWithPostfix() {
        setupTargetWithoutPostfix();
        Set<Node> allNode = allNodeWithoutPostFix();
        baseSaltJobRunner = new BaseSaltJobRunner(saltStateService, targetHostnames, allNode) {
            @Override
            public String submit(SaltConnector saltConnector) {
                return "";
            }
        };

        Set<String> returnedNodes = new HashSet<>();
        returnedNodes.add("host-10-0-0-1.example.com");
        returnedNodes.add("host-10-0-0-2.example.com");
        Set<String> collectedMissingNodes = baseSaltJobRunner.collectMissingHostnames(returnedNodes);

        assertEquals(1L, collectedMissingNodes.size());
        assertEquals("host-10-0-0-3", collectedMissingNodes.iterator().next());
    }

    @Test
    void collectMissingNodesWhenAllNodeAndSaltNodeWithoutPostfix() {
        setupTargetWithoutPostfix();
        Set<Node> allNode = allNodeWithoutPostFix();
        baseSaltJobRunner = new BaseSaltJobRunner(saltStateService, targetHostnames, allNode) {
            @Override
            public String submit(SaltConnector saltConnector) {
                return "";
            }
        };

        Set<String> returnedNodes = new HashSet<>();
        returnedNodes.add("host-10-0-0-1");
        returnedNodes.add("host-10-0-0-2");
        Set<String> collectedMissingNodes = baseSaltJobRunner.collectMissingHostnames(returnedNodes);

        assertEquals(1L, collectedMissingNodes.size());
        assertEquals("host-10-0-0-3", collectedMissingNodes.iterator().next());
    }

    @Test
    void collectMissingNodesWhenAllNodeWithPostfixAndSaltNodeWithoutPostfix() {
        setupTargetWithPostfix();
        Set<Node> allNode = allNodeWithPostFix();
        baseSaltJobRunner = new BaseSaltJobRunner(saltStateService, targetHostnames, allNode) {
            @Override
            public String submit(SaltConnector saltConnector) {
                return "";
            }
        };

        Set<String> returnedNodes = new HashSet<>();
        returnedNodes.add("host-10-0-0-1.example.com");
        returnedNodes.add("host-10-0-0-2.example.com");
        Set<String> collectedMissingNodes = baseSaltJobRunner.collectMissingHostnames(returnedNodes);

        assertEquals(1L, collectedMissingNodes.size());
        assertEquals("host-10-0-0-3.example.com", collectedMissingNodes.iterator().next());
    }

    @Test
    void collectSucceededNodesTest() {
        Set<Node> allNode = allNodeWithPostFix();
        baseSaltJobRunner = new BaseSaltJobRunner(saltStateService, targetHostnames, allNode) {
            @Override
            public String submit(SaltConnector saltConnector) {
                return "";
            }
        };
        ApplyResponse applyResponse = new ApplyResponse();
        List<Map<String, JsonNode>> resultList = new ArrayList<>();
        Map<String, JsonNode> resultMap = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        resultMap.put("host-10-0-0-1.example.com", objectMapper.valueToTree("10.0.0.1"));
        resultMap.put("host-10-0-0-2.example.com", objectMapper.valueToTree("10.0.0.2"));
        resultMap.put("host-10-0-0-3.example.com", objectMapper.valueToTree("10.0.0.3"));
        resultList.add(resultMap);
        applyResponse.setResult(resultList);
        Set<String> collectedNodes = baseSaltJobRunner.collectSucceededNodes(applyResponse);
        assertEquals(3L, collectedNodes.size());
        assertTrue(collectedNodes.contains("host-10-0-0-1.example.com"));
        assertTrue(collectedNodes.contains("host-10-0-0-2.example.com"));
        assertTrue(collectedNodes.contains("host-10-0-0-3.example.com"));
    }

    private Set<Node> allNodeWithPostFix() {
        Set<Node> allNode = new HashSet<>();
        for (int i = 1; i <= 3; i++) {
            Node node = new Node("10.0.0." + i, "88.77.66.5" + i, "i-1234" + i, "m5.xlarge-" + i, "host-10-0-0-" + i + ".example.com", "hg");
            allNode.add(node);
        }
        return allNode;
    }

    private Set<Node> allNodeWithoutPostFix() {
        Set<Node> allNode = new HashSet<>();
        for (int i = 1; i <= 3; i++) {
            Node node = new Node("10.0.0." + i, "88.77.66.5" + i, "i-1234" + i, "m5.xlarge-" + i, "host-10-0-0-" + i, "hg");
            allNode.add(node);
        }
        return allNode;
    }

    private void setupTargetWithPostfix() {
        for (int i = 1; i <= 3; i++) {
            targetHostnames.add("host-10-0-0-" + i + ".example.com");
        }
    }

    private void setupTargetWithoutPostfix() {
        for (int i = 1; i <= 3; i++) {
            targetHostnames.add("host-10-0-0-" + i);
        }
    }
}
