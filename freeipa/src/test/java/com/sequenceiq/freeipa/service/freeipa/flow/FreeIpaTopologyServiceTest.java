package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.TopologySegment;
import com.sequenceiq.freeipa.client.model.TopologySuffix;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaTopologyServiceTest {

    @InjectMocks
    private FreeIpaTopologyService underTest;

    @Mock
    private FreeIpaClient freeIpaClient;

    @Mock
    private StackService stackService;

    @Mock
    private Stack stack;

    @Test
    void testGenerateTopology() throws FreeIpaClientException {
        Set<String> nodes = new HashSet<>();
        assertSetEquivlance(underTest.generateTopology(Set.of("")), Set.of());
        nodes.add("node1");
        assertSetEquivlance(underTest.generateTopology(nodes), Set.of());
        nodes.add("node2");
        assertSetEquivlance(underTest.generateTopology(nodes), Set.of(
                new FreeIpaTopologyService.UnorderedPair("node1", "node2")));
        nodes.add("node3");
        assertSetEquivlance(underTest.generateTopology(nodes), Set.of(
                new FreeIpaTopologyService.UnorderedPair("node1", "node2"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node3"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node3")));
        nodes.add("node4");
        assertSetEquivlance(underTest.generateTopology(nodes), Set.of(
                new FreeIpaTopologyService.UnorderedPair("node1", "node2"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node3"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node4"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node3"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node4"),
                new FreeIpaTopologyService.UnorderedPair("node3", "node4")));
        nodes.add("node5");
        assertSetEquivlance(underTest.generateTopology(nodes), Set.of(
                new FreeIpaTopologyService.UnorderedPair("node1", "node2"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node3"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node4"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node5"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node3"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node4"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node5"),
                new FreeIpaTopologyService.UnorderedPair("node3", "node4"),
                new FreeIpaTopologyService.UnorderedPair("node3", "node5"),
                new FreeIpaTopologyService.UnorderedPair("node4", "node5")));
        nodes.add("node6");
        assertSetEquivlance(underTest.generateTopology(nodes), Set.of(
                new FreeIpaTopologyService.UnorderedPair("node1", "node2"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node3"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node4"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node5"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node6"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node3"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node4"),
                new FreeIpaTopologyService.UnorderedPair("node3", "node5"),
                new FreeIpaTopologyService.UnorderedPair("node3", "node6"),
                new FreeIpaTopologyService.UnorderedPair("node4", "node5"),
                new FreeIpaTopologyService.UnorderedPair("node4", "node6"),
                new FreeIpaTopologyService.UnorderedPair("node5", "node6")));
        nodes.add("node7");
        assertSetEquivlance(underTest.generateTopology(nodes), Set.of(
                new FreeIpaTopologyService.UnorderedPair("node1", "node2"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node3"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node4"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node5"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node6"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node7"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node3"),
                new FreeIpaTopologyService.UnorderedPair("node3", "node4"),
                new FreeIpaTopologyService.UnorderedPair("node3", "node5"),
                new FreeIpaTopologyService.UnorderedPair("node4", "node6"),
                new FreeIpaTopologyService.UnorderedPair("node4", "node7"),
                new FreeIpaTopologyService.UnorderedPair("node5", "node6"),
                new FreeIpaTopologyService.UnorderedPair("node5", "node7"),
                new FreeIpaTopologyService.UnorderedPair("node6", "node7")));
    }

    private void assertSetEquivlance(Set<?> s1, Set<?> s2) {
        Assertions.assertNotNull(s1);
        Assertions.assertNotNull(s2);
        Assertions.assertEquals(s1.size(), s2.size());
        Assertions.assertTrue(s1.containsAll(s2));
    }

    private static Object[][] testUpdateReplicationTopologyParameters() {
        // Parameters: numNodes, expectedSegmentsToAdd, expectedSegementsToRemove
        return new Object[][] {
                { 1, 0, 0},
                { 2, 0, 0},
                { 3, 1, 0},
                { 4, 3, 0},
                { 5, 6, 0},
                { 6, 8, 1},
                { 7, 10, 2}
        };
    }

    @MethodSource("testUpdateReplicationTopologyParameters")
    @ParameterizedTest(name = "Run {index}: numNodes={0}, expectedSegmentsToAdd={1}, expectedSegmentsToRemove={2}")
    void testUpdateReplicationTopology(int numNodes, int expectedSegmentsToAdd, int expectedSegmentsToRemove) throws FreeIpaClientException {
        Mockito.when(stackService.getByIdWithListsInTransaction(Mockito.anyLong())).thenReturn(stack);
        Set<InstanceMetaData> imSet = new HashSet<>();
        for (int i = 0; i < numNodes; i++) {
            InstanceMetaData im = new InstanceMetaData();
            im.setDiscoveryFQDN(String.format("ipaserver%d.example.com", i));
            imSet.add(im);
        }
        Mockito.when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(imSet);
        TopologySuffix caSuffix = new TopologySuffix();
        caSuffix.setCn("ca");
        TopologySuffix domainSuffix = new TopologySuffix();
        domainSuffix.setCn("domain");
        Mockito.when(freeIpaClient.findAllTopologySuffixes()).thenReturn(List.of(caSuffix, domainSuffix));
        List<TopologySegment> topologySegments1 = new LinkedList<>();
        for (int i = 1; i < numNodes; i++) {
            TopologySegment segment = new TopologySegment();
            segment.setLeftNode("ipaserver0.example.com");
            segment.setRightNode(String.format("ipaserver%d.example.com", i));
            topologySegments1.add(segment);
        }
        List<TopologySegment> topologySegments2 = new LinkedList<>();
        topologySegments2.addAll(topologySegments1);
        Mockito.when(freeIpaClient.findTopologySegments(Mockito.anyString()))
                .thenReturn(topologySegments1)
                .thenReturn(topologySegments1)
                .thenReturn(topologySegments2)
                .thenReturn(topologySegments2);
        if (expectedSegmentsToAdd > 0) {
            Mockito.when(freeIpaClient.addTopologySegment(Mockito.anyString(), Mockito.any())).thenReturn(new TopologySegment());
        }
        if (expectedSegmentsToRemove > 0) {
            Mockito.when(freeIpaClient.deleteTopologySegment(Mockito.anyString(), Mockito.any())).thenReturn(new TopologySegment());
        }
        underTest.updateReplicationTopology(1L, Set.of(), freeIpaClient);
        Mockito.verify(freeIpaClient, Mockito.times(expectedSegmentsToAdd)).addTopologySegment(Mockito.eq("ca"), Mockito.any());
        Mockito.verify(freeIpaClient, Mockito.times(expectedSegmentsToAdd)).addTopologySegment(Mockito.eq("domain"), Mockito.any());
        Mockito.verify(freeIpaClient, Mockito.times(expectedSegmentsToRemove)).deleteTopologySegment(Mockito.eq("ca"), Mockito.any());
        Mockito.verify(freeIpaClient, Mockito.times(expectedSegmentsToRemove)).deleteTopologySegment(Mockito.eq("domain"), Mockito.any());
    }

    @MethodSource("testUpdateReplicationTopologyParameters")
    void testUpdateReplicationTopologyForDownscale() throws FreeIpaClientException {
        Mockito.when(stackService.getByIdWithListsInTransaction(Mockito.anyLong())).thenReturn(stack);
        InstanceMetaData im1 = new InstanceMetaData();
        InstanceMetaData im2 = new InstanceMetaData();
        im1.setDiscoveryFQDN("ipaserver1.example.com");
        im2.setDiscoveryFQDN("ipaserver2.example.com");
        Set<InstanceMetaData> imSet = Set.of(im1, im2);
        Mockito.when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(imSet);
        TopologySuffix caSuffix = new TopologySuffix();
        caSuffix.setCn("ca");
        Mockito.when(freeIpaClient.findAllTopologySuffixes()).thenReturn(List.of(caSuffix));

        Mockito.when(freeIpaClient.deleteTopologySegment(Mockito.anyString(), Mockito.any())).thenReturn(new TopologySegment());
        underTest.updateReplicationTopology(1L, Set.of("ipaserver2.example.com"), freeIpaClient);
        Mockito.verify(freeIpaClient, Mockito.never()).addTopologySegment(Mockito.any(), Mockito.any());
        Mockito.verify(freeIpaClient, Mockito.times(1)).deleteTopologySegment(Mockito.eq("ca"), Mockito.any());
    }

}