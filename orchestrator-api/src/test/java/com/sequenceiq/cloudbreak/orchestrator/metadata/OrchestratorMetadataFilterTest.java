package com.sequenceiq.cloudbreak.orchestrator.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.orchestration.Node;

public class OrchestratorMetadataFilterTest {

    @Test
    public void testFilterMetadataWithIncludeHosts() {
        // GIVEN
        Set<String> hosts = new HashSet<>();
        hosts.add("host1");
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .includeHosts(hosts).build();
        // WHEN
        OrchestratorMetadata result = filter.apply(metadata(createNode()));
        // THEN
        assertEquals(1, result.getNodes().size());
    }

    @Test
    public void testFilterMetadataWithExcludeHosts() {
        // GIVEN
        Set<String> excludeHosts = new HashSet<>();
        excludeHosts.add("host1");
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .exlcudeHosts(excludeHosts).build();
        // WHEN
        OrchestratorMetadata result = filter.apply(metadata(createNode()));
        // THEN
        assertTrue(result.getNodes().isEmpty());
    }

    @Test
    public void testFilterNodesWithExcludedHostsWithDifferentHost() {
        // GIVEN
        Set<String> excludeHosts = new HashSet<>();
        excludeHosts.add("host2");
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .exlcudeHosts(excludeHosts).build();
        // WHEN
        OrchestratorMetadata result = filter.apply(metadata(createNode()));
        // THEN
        assertEquals(1, result.getNodes().size());
    }

    @Test
    public void testFilterNodesWithPredefinedNodes() {
        // GIVEN
        Set<String> hosts = new HashSet<>();
        hosts.add("host1");
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .includeHosts(hosts).build();
        // WHEN
        Set<Node> result = filter.apply(Set.of(createNode()));
        // THEN
        assertEquals(1, result.size());
    }

    @Test
    public void testFilterNodesWithWrongHosts() {
        // GIVEN
        Set<String> hosts = new HashSet<>();
        hosts.add("host2");
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .includeHosts(hosts).build();
        // WHEN
        OrchestratorMetadata result = filter.apply(metadata(createNode()));
        // THEN
        assertTrue(result.getNodes().isEmpty());
    }

    @Test
    public void testFilterNodesWithHostGroups() {
        // GIVEN
        Set<String> hostGroups = new HashSet<>();
        hostGroups.add("master");
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .includeHostGroups(hostGroups)
                .build();
        // WHEN
        OrchestratorMetadata result = filter.apply(metadata(createNode()));
        // THEN
        assertEquals(1, result.getNodes().size());
    }

    @Test
    public void testFilterNodesWithWrongHostGroup() {
        // GIVEN
        Set<String> hostGroups = new HashSet<>();
        hostGroups.add("idbroker");
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .includeHostGroups(hostGroups)
                .build();
        // WHEN
        OrchestratorMetadata result = filter.apply(metadata(createNode()));
        // THEN
        assertTrue(result.getNodes().isEmpty());
    }

    @Test
    public void testFilterNodesWithHostAndHostGroup() {
        // GIVEN
        Set<String> hostGroups = new HashSet<>();
        hostGroups.add("master");
        Set<String> hosts = new HashSet<>();
        hosts.add("host1");
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .includeHosts(hosts)
                .includeHostGroups(hostGroups)
                .build();
        // WHEN
        OrchestratorMetadata result = filter.apply(metadata(createNode()));
        // THEN
        assertEquals(1, result.getNodes().size());
    }

    @Test
    public void testFilterNodesWithHostAndWrongHostGroupFilter() {
        // GIVEN
        Set<String> hostGroups = new HashSet<>();
        hostGroups.add("idbroker");
        Set<String> hosts = new HashSet<>();
        hosts.add("host1");
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .includeHosts(hosts)
                .includeHostGroups(hostGroups)
                .build();
        // WHEN
        OrchestratorMetadata result = filter.apply(metadata(createNode()));
        // THEN
        assertTrue(result.getNodes().isEmpty());
    }

    @Test
    public void testFilterNodesWithWrongHostAndHostGroup() {
        // GIVEN
        Set<String> hostGroups = new HashSet<>();
        hostGroups.add("master");
        Set<String> hosts = new HashSet<>();
        hosts.add("host2");
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .includeHosts(hosts)
                .includeHostGroups(hostGroups)
                .build();
        // WHEN
        OrchestratorMetadata result = filter.apply(metadata(createNode()));
        // THEN
        assertTrue(result.getNodes().isEmpty());
    }

    @Test
    public void testFilterNodesWithEmptyFilters() {
        // GIVEN
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .build();
        // WHEN
        OrchestratorMetadata result = filter.apply(metadata(createNode()));
        // THEN
        assertEquals(1, result.getNodes().size());
    }

    @Test
    public void testFilterNodesWithExcludedHostsWithPrecedence() {
        // GIVEN
        Set<String> hosts = new HashSet<>();
        hosts.add("host2");
        Set<String> excludeHosts = new HashSet<>();
        excludeHosts.add("host1");
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .includeHosts(hosts)
                .exlcudeHosts(excludeHosts)
                .build();
        // WHEN
        OrchestratorMetadata result = filter.apply(metadata(createNode()));
        // THEN
        assertTrue(result.getNodes().isEmpty());
    }

    private Node createNode() {
        return new Node("privateIp", "publicIp", "instanceId",
                "instanceType", "host1", "master");
    }

    private OrchestratorMetadata metadata(Node node) {
        return new OrchestratorMetadata(null, Set.of(node), null, null);
    }
}
