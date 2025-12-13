package com.sequenceiq.freeipa.service.freeipa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.TopologySegment;
import com.sequenceiq.freeipa.client.model.TopologySuffix;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
class FreeIpaTopologyServiceTest {

    private static final int NOT_FOUND = 4001;

    @InjectMocks
    private FreeIpaTopologyService underTest;

    @Mock
    private FreeIpaClient freeIpaClient;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Test
    void testGenerateTopology() {
        Set<String> nodes = new HashSet<>();
        assertSetEquivalence(underTest.generateTopology(Set.of("")), Set.of());
        nodes.add("node1");
        assertSetEquivalence(underTest.generateTopology(nodes), Set.of());
        nodes.add("node2");
        assertSetEquivalence(underTest.generateTopology(nodes), Set.of(
                new FreeIpaTopologyService.UnorderedPair("node1", "node2")));
        nodes.add("node3");
        assertSetEquivalence(underTest.generateTopology(nodes), Set.of(
                new FreeIpaTopologyService.UnorderedPair("node1", "node2"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node3"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node3")));
        nodes.add("node4");
        assertSetEquivalence(underTest.generateTopology(nodes), Set.of(
                new FreeIpaTopologyService.UnorderedPair("node1", "node2"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node3"),
                new FreeIpaTopologyService.UnorderedPair("node1", "node4"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node3"),
                new FreeIpaTopologyService.UnorderedPair("node2", "node4"),
                new FreeIpaTopologyService.UnorderedPair("node3", "node4")));
        nodes.add("node5");
        assertSetEquivalence(underTest.generateTopology(nodes), Set.of(
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
        assertSetEquivalence(underTest.generateTopology(nodes), Set.of(
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
        assertSetEquivalence(underTest.generateTopology(nodes), Set.of(
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

    private void assertSetEquivalence(Set<?> s1, Set<?> s2) {
        assertNotNull(s1);
        assertNotNull(s2);
        assertEquals(s1.size(), s2.size());
        assertTrue(s1.containsAll(s2));
    }

    private static Object[][] testUpdateReplicationTopologyParameters() {
        // Parameters: numNodes, expectedSegmentsToAdd, expectedSegementsToRemove
        return new Object[][]{
                {1, 0, 0},
                {2, 0, 0},
                {3, 1, 0},
                {4, 3, 0},
                {5, 6, 0},
                {6, 8, 1},
                {7, 10, 2}
        };
    }

    @MethodSource("testUpdateReplicationTopologyParameters")
    @ParameterizedTest(name = "Run {index}: numNodes={0}, expectedSegmentsToAdd={1}, expectedSegmentsToRemove={2}")
    void testUpdateReplicationTopology(int numNodes, int expectedSegmentsToAdd, int expectedSegmentsToRemove) throws FreeIpaClientException {
        Set<InstanceMetaData> imSet = new HashSet<>();
        for (int i = 0; i < numNodes; i++) {
            InstanceMetaData im = new InstanceMetaData();
            im.setDiscoveryFQDN(String.format("ipaserver%d.example.com", i));
            imSet.add(im);
        }
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong())).thenReturn(imSet);
        TopologySuffix caSuffix = new TopologySuffix();
        caSuffix.setCn("ca");
        TopologySuffix domainSuffix = new TopologySuffix();
        domainSuffix.setCn("domain");
        when(freeIpaClient.findAllTopologySuffixes()).thenReturn(List.of(caSuffix, domainSuffix));
        List<TopologySegment> topologySegments1 = new LinkedList<>();
        for (int i = 1; i < numNodes; i++) {
            TopologySegment segment = new TopologySegment();
            segment.setLeftNode("ipaserver0.example.com");
            segment.setRightNode(String.format("ipaserver%d.example.com", i));
            topologySegments1.add(segment);
        }
        List<TopologySegment> topologySegments2 = new LinkedList<>();
        topologySegments2.addAll(topologySegments1);
        when(freeIpaClient.findTopologySegments(anyString()))
                .thenReturn(topologySegments1)
                .thenReturn(topologySegments1)
                .thenReturn(topologySegments2)
                .thenReturn(topologySegments2);
        if (expectedSegmentsToAdd > 0) {
            when(freeIpaClient.addTopologySegment(anyString(), any())).thenReturn(new TopologySegment());
        }
        if (expectedSegmentsToRemove > 0) {
            when(freeIpaClient.deleteTopologySegment(anyString(), any())).thenReturn(new TopologySegment());
        }
        underTest.updateReplicationTopology(1L, Set.of(), freeIpaClient);
        verify(freeIpaClient, times(expectedSegmentsToAdd)).addTopologySegment(eq("ca"), any());
        verify(freeIpaClient, times(expectedSegmentsToAdd)).addTopologySegment(eq("domain"), any());
        verify(freeIpaClient, times(expectedSegmentsToRemove)).deleteTopologySegment(eq("ca"), any());
        verify(freeIpaClient, times(expectedSegmentsToRemove)).deleteTopologySegment(eq("domain"), any());
    }

    @MethodSource("testUpdateReplicationTopologyParameters")
    @ParameterizedTest(name = "Run {index}: numNodes={0}, expectedSegmentsToAdd={1}, expectedSegmentsToRemove={2}")
    void testUpdateReplicationTopologyWithRetry(int numNodes, int expectedSegmentsToAdd, int expectedSegmentsToRemove) throws FreeIpaClientException {
        Stack stack = new Stack();
        stack.setId(1L);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        Set<InstanceMetaData> imSet = new HashSet<>();
        for (int i = 0; i < numNodes; i++) {
            InstanceMetaData im = new InstanceMetaData();
            im.setDiscoveryFQDN(String.format("ipaserver%d.example.com", i));
            imSet.add(im);
        }
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong())).thenReturn(imSet);
        TopologySuffix caSuffix = new TopologySuffix();
        caSuffix.setCn("ca");
        TopologySuffix domainSuffix = new TopologySuffix();
        domainSuffix.setCn("domain");
        when(freeIpaClient.findAllTopologySuffixes()).thenReturn(List.of(caSuffix, domainSuffix));
        List<TopologySegment> topologySegments1 = new LinkedList<>();
        for (int i = 1; i < numNodes; i++) {
            TopologySegment segment = new TopologySegment();
            segment.setLeftNode("ipaserver0.example.com");
            segment.setRightNode(String.format("ipaserver%d.example.com", i));
            topologySegments1.add(segment);
        }
        List<TopologySegment> topologySegments2 = new LinkedList<>();
        topologySegments2.addAll(topologySegments1);
        when(freeIpaClient.findTopologySegments(anyString()))
                .thenReturn(topologySegments1)
                .thenReturn(topologySegments1)
                .thenReturn(topologySegments2)
                .thenReturn(topologySegments2);
        if (expectedSegmentsToAdd > 0) {
            when(freeIpaClient.addTopologySegment(anyString(), any())).thenReturn(new TopologySegment());
        }
        if (expectedSegmentsToRemove > 0) {
            when(freeIpaClient.deleteTopologySegment(anyString(), any())).thenReturn(new TopologySegment());
        }
        underTest.updateReplicationTopologyWithRetry(stack, Set.of());
        verify(freeIpaClient, times(expectedSegmentsToAdd)).addTopologySegment(eq("ca"), any());
        verify(freeIpaClient, times(expectedSegmentsToAdd)).addTopologySegment(eq("domain"), any());
        verify(freeIpaClient, times(expectedSegmentsToRemove)).deleteTopologySegment(eq("ca"), any());
        verify(freeIpaClient, times(expectedSegmentsToRemove)).deleteTopologySegment(eq("domain"), any());
    }

    @Test
    void testUpdateReplicationTopologyForDownscale() throws FreeIpaClientException {
        InstanceMetaData im1 = new InstanceMetaData();
        InstanceMetaData im2 = new InstanceMetaData();
        im1.setDiscoveryFQDN("ipaserver1.example.com");
        im2.setDiscoveryFQDN("ipaserver2.example.com");
        Set<InstanceMetaData> imSet = Set.of(im1, im2);
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong())).thenReturn(imSet);
        TopologySuffix caSuffix = new TopologySuffix();
        caSuffix.setCn("ca");
        when(freeIpaClient.findAllTopologySuffixes()).thenReturn(List.of(caSuffix));
        List<TopologySegment> topologySegments = new LinkedList<>();
        TopologySegment segment = new TopologySegment();
        segment.setLeftNode("ipaserver1.example.com");
        segment.setRightNode("ipaserver2.example.com");
        topologySegments.add(segment);
        when(freeIpaClient.findTopologySegments(anyString())).thenReturn(topologySegments);
        when(freeIpaClient.deleteTopologySegment(anyString(), any())).thenReturn(new TopologySegment());

        underTest.updateReplicationTopology(1L, Set.of("ipaserver2.example.com"), freeIpaClient);

        verify(freeIpaClient, never()).addTopologySegment(any(), any());
        verify(freeIpaClient, times(1)).deleteTopologySegment(eq("ca"), any());
    }

    @Test
    void testUpdateReplicationTopologyForDownscaleAndSegmentIsNotPresent() throws FreeIpaClientException {
        InstanceMetaData im1 = new InstanceMetaData();
        InstanceMetaData im2 = new InstanceMetaData();
        im1.setDiscoveryFQDN("ipaserver1.example.com");
        im2.setDiscoveryFQDN("ipaserver2.example.com");
        Set<InstanceMetaData> imSet = Set.of(im1, im2);
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong())).thenReturn(imSet);
        TopologySuffix caSuffix = new TopologySuffix();
        caSuffix.setCn("ca");
        when(freeIpaClient.findAllTopologySuffixes()).thenReturn(List.of(caSuffix));
        List<TopologySegment> topologySegments = new LinkedList<>();
        TopologySegment segment = new TopologySegment();
        segment.setLeftNode("ipaserver1.example.com");
        segment.setRightNode("ipaserver2.example.com");
        topologySegments.add(segment);
        when(freeIpaClient.findTopologySegments(anyString())).thenReturn(topologySegments);
        String message = "already deleted";
        when(freeIpaClient.deleteTopologySegment(anyString(), any()))
                .thenThrow(new FreeIpaClientException(message, new JsonRpcClientException(NOT_FOUND, message, null)));

        underTest.updateReplicationTopology(1L, Set.of("ipaserver2.example.com"), freeIpaClient);

        verify(freeIpaClient, never()).addTopologySegment(any(), any());
        verify(freeIpaClient, times(1)).deleteTopologySegment(eq("ca"), any());
    }

}