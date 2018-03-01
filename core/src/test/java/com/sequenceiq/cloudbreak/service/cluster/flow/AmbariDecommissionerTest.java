package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariDecommissioner;

@RunWith(MockitoJUnitRunner.class)
public class AmbariDecommissionerTest {

    private AmbariDecommissioner underTest = new AmbariDecommissioner();

    @Test
    public void testSelectNodesWhenHasOneUnhealthyNodeAndShouldSelectOne() {

        String hostname1 = "10.0.0.1";
        String hostname2 = "10.0.0.2";

        HostMetadata unhealhtyNode = getHostMetadata(hostname1, HostMetadataState.UNHEALTHY);
        HostMetadata healhtyNode = getHostMetadata(hostname2, HostMetadataState.HEALTHY);

        Collection<HostMetadata> nodes = Arrays.asList(unhealhtyNode, healhtyNode);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname1, 100L);
        ascendingNodes.put(hostname2, 110L);

        Map<String, Long> selectedNodes = underTest.selectNodes(ascendingNodes, nodes, 1);

        Assert.assertEquals(1, selectedNodes.size());
        Assert.assertEquals(hostname1, selectedNodes.keySet().stream().findFirst().get());
    }

    @Test
    public void testSelectNodesWhenHasOneUnhealthyNodeAndShouldSelectTwo() {

        String hostname1 = "10.0.0.1";
        String hostname2 = "10.0.0.2";
        String hostname3 = "10.0.0.3";

        HostMetadata unhealhtyNode = getHostMetadata(hostname1, HostMetadataState.UNHEALTHY);
        HostMetadata healhtyNode1 = getHostMetadata(hostname2, HostMetadataState.HEALTHY);
        HostMetadata healhtyNode2 = getHostMetadata(hostname3, HostMetadataState.HEALTHY);

        List<HostMetadata> nodes = Arrays.asList(unhealhtyNode, healhtyNode1, healhtyNode2);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname1, 100L);
        ascendingNodes.put(hostname2, 110L);
        ascendingNodes.put(hostname3, 120L);

        Map<String, Long> selectedNodes = underTest.selectNodes(ascendingNodes, nodes, 2);

        Assert.assertEquals(2, selectedNodes.size());
        Assert.assertTrue(selectedNodes.keySet().containsAll(Arrays.asList(hostname1, hostname2)));
    }

    @Test
    public void testSelectNodesWhenHasThreeUnhealthyNodeAndShouldSelectTwo() {

        String hostname1 = "10.0.0.1";
        String hostname2 = "10.0.0.2";
        String hostname3 = "10.0.0.3";
        String hostname4 = "10.0.0.4";
        String hostname5 = "10.0.0.5";

        HostMetadata unhealhtyNode1 = getHostMetadata(hostname1, HostMetadataState.UNHEALTHY);
        HostMetadata unhealhtyNode2 = getHostMetadata(hostname2, HostMetadataState.UNHEALTHY);
        HostMetadata unhealhtyNode3 = getHostMetadata(hostname3, HostMetadataState.UNHEALTHY);
        HostMetadata healhtyNode1 = getHostMetadata(hostname4, HostMetadataState.HEALTHY);
        HostMetadata healhtyNode2 = getHostMetadata(hostname5, HostMetadataState.HEALTHY);

        List<HostMetadata> nodes = Arrays.asList(unhealhtyNode1, unhealhtyNode2, unhealhtyNode3, healhtyNode1, healhtyNode2);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname1, 100L);
        ascendingNodes.put(hostname2, 110L);
        ascendingNodes.put(hostname3, 120L);
        ascendingNodes.put(hostname4, 130L);
        ascendingNodes.put(hostname5, 140L);

        Map<String, Long> selectedNodes = underTest.selectNodes(ascendingNodes, nodes, 2);

        Assert.assertEquals(2, selectedNodes.size());
        Assert.assertTrue(selectedNodes.keySet().containsAll(Arrays.asList(hostname1, hostname2)));
    }

    @Test
    public void testSelectNodesWhenHasOneUnhealthyNodeButNotInAscendingList() {

        String hostname1 = "10.0.0.1";
        String hostname2 = "10.0.0.2";
        String hostname3 = "10.0.0.3";

        HostMetadata unhealhtyNode1 = getHostMetadata(hostname1, HostMetadataState.UNHEALTHY);
        HostMetadata healhtyNode1 = getHostMetadata(hostname2, HostMetadataState.HEALTHY);
        HostMetadata healhtyNode2 = getHostMetadata(hostname3, HostMetadataState.HEALTHY);

        List<HostMetadata> nodes = Arrays.asList(unhealhtyNode1, healhtyNode1, healhtyNode2);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname2, 110L);
        ascendingNodes.put(hostname3, 120L);

        Map<String, Long> selectedNodes = underTest.selectNodes(ascendingNodes, nodes, 1);

        Assert.assertEquals(1, selectedNodes.size());
        Assert.assertTrue(selectedNodes.keySet().contains(hostname2));
    }

    @Test
    public void testSelectNodesWhenHostNameShouldContainsInAscNodesAndNodes() {

        String hostname1 = "10.0.0.1";

        HostMetadata healhtyNode1 = getHostMetadata(hostname1, HostMetadataState.HEALTHY);

        List<HostMetadata> nodes = Collections.singletonList(healhtyNode1);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname1, 100L);

        Map<String, Long> selectedNodes =  underTest.selectNodes(ascendingNodes, nodes, 1);

        Assert.assertEquals(1, selectedNodes.size());
        Assert.assertTrue(selectedNodes.keySet().contains(hostname1));
    }

    private HostMetadata getHostMetadata(String hostname2, HostMetadataState state) {
        HostMetadata healhtyNode = new HostMetadata();
        healhtyNode.setHostName(hostname2);
        healhtyNode.setHostMetadataState(state);
        return healhtyNode;
    }
}
