package com.sequenceiq.cloudbreak.service.ha;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.domain.CloudbreakNode;
import com.sequenceiq.cloudbreak.domain.FlowLog;

@RunWith(MockitoJUnitRunner.class)
public class EvenFlowDistributorTest {

    private static final String MY_ID = "E80C7BD9-61CD-442E-AFDA-C3B30FEDE88F";

    private static final String NODE_1_ID = "5575B7AD-45CB-487D-BE14-E33C913F9394";

    private static final String NODE_2_ID = "854506AC-A0D5-4C98-A47C-70F6251FC604";

    private static final String NODE_3_ID = "65B623B9-9FE7-41F4-95A5-848DCB0C108E";

    private EvenFlowDistributor flowDistributor = new EvenFlowDistributor();

    @Test
    public void testOddFlowDistribution() {
        List<CloudbreakNode> nodes = getClusterNodes();
        List<String> flowLogs = getFlowIds(9);
        Map<CloudbreakNode, List<String>> result = flowDistributor.distribute(flowLogs, nodes);
        assertEquals(3, result.get(nodes.get(0)).size());
        assertEquals(2, result.get(nodes.get(1)).size());
        assertEquals(2, result.get(nodes.get(2)).size());
        assertEquals(2, result.get(nodes.get(3)).size());
    }

    @Test
    public void testEvenFlowDistribution() {
        List<CloudbreakNode> nodes = getClusterNodes();
        List<String> flowLogs = getFlowIds(12);
        Map<CloudbreakNode, List<String>> result = flowDistributor.distribute(flowLogs, nodes);
        assertEquals(3, result.get(nodes.get(0)).size());
        assertEquals(3, result.get(nodes.get(1)).size());
        assertEquals(3, result.get(nodes.get(2)).size());
        assertEquals(3, result.get(nodes.get(3)).size());
    }

    @Test
    public void testEvenFlowOddNodesDistribution() {
        List<CloudbreakNode> nodes = getClusterNodes();
        nodes.remove(3);
        List<String> flowLogs = getFlowIds(12);
        Map<CloudbreakNode, List<String>> result = flowDistributor.distribute(flowLogs, nodes);
        assertEquals(4, result.get(nodes.get(0)).size());
        assertEquals(4, result.get(nodes.get(1)).size());
        assertEquals(4, result.get(nodes.get(2)).size());
    }

    @Test
    public void testOddFlowOddNodesDistribution() {
        List<CloudbreakNode> nodes = getClusterNodes();
        nodes.remove(3);
        List<String> flowLogs = getFlowIds(11);
        Map<CloudbreakNode, List<String>> result = flowDistributor.distribute(flowLogs, nodes);
        assertEquals(4, result.get(nodes.get(0)).size());
        assertEquals(4, result.get(nodes.get(1)).size());
        assertEquals(3, result.get(nodes.get(2)).size());
    }

    @Test
    public void testFlowDistributionSingleNode() {
        CloudbreakNode node = new CloudbreakNode(MY_ID);
        List<String> flowLogs = getFlowIds(11);
        Map<CloudbreakNode, List<String>> result = flowDistributor.distribute(flowLogs, Collections.singletonList(node));
        assertEquals(11, result.get(node).size());
    }

    private List<CloudbreakNode> getClusterNodes() {
        List<CloudbreakNode> nodes = new ArrayList<>();
        nodes.add(new CloudbreakNode(MY_ID));
        nodes.add(new CloudbreakNode(NODE_1_ID));
        nodes.add(new CloudbreakNode(NODE_2_ID));
        nodes.add(new CloudbreakNode(NODE_3_ID));
        return nodes;
    }

    private List<String> getFlowIds(int flowCount) {
        return getFlowLogs(flowCount).stream().map(FlowLog::getFlowId).collect(Collectors.toList());
    }

    private List<FlowLog> getFlowLogs(int flowCount) {
        List<FlowLog> flows = new ArrayList<>();
        Random random = new Random(System.currentTimeMillis());
        int flowId = random.nextInt(5000);
        long stackId = random.nextLong();
        for (int i = 0; i < flowCount; i++) {
            flows.add(new FlowLog(stackId + i, "" + flowId + i, "RUNNING", false));
        }
        return flows;
    }
}
