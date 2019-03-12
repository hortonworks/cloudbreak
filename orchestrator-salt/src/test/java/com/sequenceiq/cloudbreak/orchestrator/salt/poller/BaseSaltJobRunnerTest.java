package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;

public class BaseSaltJobRunnerTest {

    private Set<String> targets = new HashSet<>();

    private BaseSaltJobRunner baseSaltJobRunner;

    @Before
    public void setUp() {
        targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
    }

    @Test
    public void collectMissingNodesAllNodeAndSaltNodeWithPrefix() {
        Set<Node> allNode = allNodeWithPostFix();
        baseSaltJobRunner = new BaseSaltJobRunner(targets, allNode) {
            @Override
            public String submit(SaltConnector saltConnector) {
                return "";
            }
        };

        Set<String> returnedNodes = new HashSet<>();
        returnedNodes.add("host-10-0-0-1.example.com");
        returnedNodes.add("host-10-0-0-2.example.com");
        Set<String> collectedMissingNodes = baseSaltJobRunner.collectMissingNodes(returnedNodes);

        Assert.assertEquals(1L, collectedMissingNodes.size());
        Assert.assertEquals("10.0.0.3", collectedMissingNodes.iterator().next());
    }

    @Test
    public void collectMissingNodesAllNodeWithoutPrefixSaltNodeWithPrefix() {
        Set<Node> allNode = allNodeWithoutPostFix();
        baseSaltJobRunner = new BaseSaltJobRunner(targets, allNode) {
            @Override
            public String submit(SaltConnector saltConnector) {
                return "";
            }
        };

        Set<String> returnedNodes = new HashSet<>();
        returnedNodes.add("host-10-0-0-1.example.com");
        returnedNodes.add("host-10-0-0-2.example.com");
        Set<String> collectedMissingNodes = baseSaltJobRunner.collectMissingNodes(returnedNodes);

        Assert.assertEquals(1L, collectedMissingNodes.size());
        Assert.assertEquals("10.0.0.3", collectedMissingNodes.iterator().next());
    }

    @Test
    public void collectMissingNodesAllNodeAndSaltNodeWithoutPrefix() {
        Set<Node> allNode = allNodeWithoutPostFix();
        baseSaltJobRunner = new BaseSaltJobRunner(targets, allNode) {
            @Override
            public String submit(SaltConnector saltConnector) {
                return "";
            }
        };

        Set<String> returnedNodes = new HashSet<>();
        returnedNodes.add("host-10-0-0-1");
        returnedNodes.add("host-10-0-0-2");
        Set<String> collectedMissingNodes = baseSaltJobRunner.collectMissingNodes(returnedNodes);

        Assert.assertEquals(1L, collectedMissingNodes.size());
        Assert.assertEquals("10.0.0.3", collectedMissingNodes.iterator().next());
    }

    @Test
    public void collectMissingNodesAllNodeWithPrefixAndSaltNodeWithoutPrefix() {
        Set<Node> allNode = allNodeWithoutPostFix();
        baseSaltJobRunner = new BaseSaltJobRunner(targets, allNode) {
            @Override
            public String submit(SaltConnector saltConnector) {
                return "";
            }
        };

        Set<String> returnedNodes = new HashSet<>();
        returnedNodes.add("host-10-0-0-1.example.com");
        returnedNodes.add("host-10-0-0-2.example.com");
        Set<String> collectedMissingNodes = baseSaltJobRunner.collectMissingNodes(returnedNodes);

        Assert.assertEquals(1L, collectedMissingNodes.size());
        Assert.assertEquals("10.0.0.3", collectedMissingNodes.iterator().next());
    }

    @Test
    public void collectNodesTest() {
        Set<Node> allNode = allNodeWithPostFix();
        baseSaltJobRunner = new BaseSaltJobRunner(targets, allNode) {
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
        Set<String> collectedNodes = baseSaltJobRunner.collectNodes(applyResponse);
        Assert.assertEquals(3L, collectedNodes.size());
        Assert.assertTrue(collectedNodes.contains("host-10-0-0-1.example.com"));
        Assert.assertTrue(collectedNodes.contains("host-10-0-0-2.example.com"));
        Assert.assertTrue(collectedNodes.contains("host-10-0-0-3.example.com"));
    }

    private Set<Node> allNodeWithPostFix() {
        Set<Node> allNode = new HashSet<>();
        for (int i = 1; i <= 3; i++) {
            Node node = new Node("10.0.0." + i, "88.77.66.5" + i, "host-10-0-0-" + i + ".example.com", "hg");
            allNode.add(node);
        }
        return allNode;
    }

    private Set<Node> allNodeWithoutPostFix() {
        Set<Node> allNode = new HashSet<>();
        for (int i = 1; i <= 3; i++) {
            Node node = new Node("10.0.0." + i, "88.77.66.5" + i, "host-10-0-0-" + i, "hg");
            allNode.add(node);
        }
        return allNode;
    }
}