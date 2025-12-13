package com.sequenceiq.cloudbreak.service.ha;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.ha.domain.Node;
import com.sequenceiq.cloudbreak.ha.service.EvenFlowDistributor;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;

@ExtendWith(MockitoExtension.class)
class EvenFlowDistributorTest {

    private static final String MY_ID = "E80C7BD9-61CD-442E-AFDA-C3B30FEDE88F";

    private static final String NODE_1_ID = "5575B7AD-45CB-487D-BE14-E33C913F9394";

    private static final String NODE_2_ID = "854506AC-A0D5-4C98-A47C-70F6251FC604";

    private static final String NODE_3_ID = "65B623B9-9FE7-41F4-95A5-848DCB0C108E";

    private final EvenFlowDistributor flowDistributor = new EvenFlowDistributor();

    @Test
    void testOddFlowDistribution() {
        List<Node> nodes = getClusterNodes();
        List<String> flowLogs = getFlowIds(9);
        Map<Node, List<String>> result = flowDistributor.distribute(flowLogs, nodes);
        assertEquals(3L, result.get(nodes.get(0)).size());
        assertEquals(2L, result.get(nodes.get(1)).size());
        assertEquals(2L, result.get(nodes.get(2)).size());
        assertEquals(2L, result.get(nodes.get(3)).size());
    }

    @Test
    void testEvenFlowDistribution() {
        List<Node> nodes = getClusterNodes();
        List<String> flowLogs = getFlowIds(12);
        Map<Node, List<String>> result = flowDistributor.distribute(flowLogs, nodes);
        assertEquals(3L, result.get(nodes.get(0)).size());
        assertEquals(3L, result.get(nodes.get(1)).size());
        assertEquals(3L, result.get(nodes.get(2)).size());
        assertEquals(3L, result.get(nodes.get(3)).size());
    }

    @Test
    void testEvenFlowOddNodesDistribution() {
        List<Node> nodes = getClusterNodes();
        nodes.remove(3);
        List<String> flowLogs = getFlowIds(12);
        Map<Node, List<String>> result = flowDistributor.distribute(flowLogs, nodes);
        assertEquals(4L, result.get(nodes.get(0)).size());
        assertEquals(4L, result.get(nodes.get(1)).size());
        assertEquals(4L, result.get(nodes.get(2)).size());
    }

    @Test
    void testOddFlowOddNodesDistribution() {
        List<Node> nodes = getClusterNodes();
        nodes.remove(3);
        List<String> flowLogs = getFlowIds(11);
        Map<Node, List<String>> result = flowDistributor.distribute(flowLogs, nodes);
        assertEquals(4L, result.get(nodes.get(0)).size());
        assertEquals(4L, result.get(nodes.get(1)).size());
        assertEquals(3L, result.get(nodes.get(2)).size());
    }

    @Test
    void testFlowDistributionSingleNode() {
        Node node = new Node(MY_ID);
        List<String> flowLogs = getFlowIds(11);
        Map<Node, List<String>> result = flowDistributor.distribute(flowLogs, Collections.singletonList(node));
        assertEquals(11L, result.get(node).size());
    }

    private List<Node> getClusterNodes() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(MY_ID));
        nodes.add(new Node(NODE_1_ID));
        nodes.add(new Node(NODE_2_ID));
        nodes.add(new Node(NODE_3_ID));
        return nodes;
    }

    private List<String> getFlowIds(int flowCount) {
        return getFlowLogs(flowCount).stream().map(FlowLog::getFlowId).collect(Collectors.toList());
    }

    private List<FlowLog> getFlowLogs(int flowCount) {
        List<FlowLog> flows = new ArrayList<>();
        Random random = new SecureRandom();
        int flowId = random.nextInt(5000);
        long stackId = random.nextLong();
        for (int i = 0; i < flowCount; i++) {
            flows.add(new FlowLog(stackId + i, "" + flowId + i, "RUNNING",
                    false, StateStatus.PENDING, OperationType.UNKNOWN));
        }
        return flows;
    }
}
