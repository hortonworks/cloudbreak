package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.orchestration.Node;

public class DiagnosticsFlowServiceTest {

    private DiagnosticsFlowService underTest;

    @BeforeEach
    public void setUp() {
        underTest = new DiagnosticsFlowService();
    }

    @Test
    public void testFilterNodesWithHostsFilter() {
        // GIVEN
        Set<String> hosts = new HashSet<>();
        hosts.add("host1");
        // WHEN
        boolean result = underTest.filterNodes(createNode(), hosts, new HashSet<>(), new HashSet<>());
        // THEN
        assertTrue(result);
    }

    @Test
    public void testFilterNodesWithWrongHostsFilter() {
        // GIVEN
        Set<String> hosts = new HashSet<>();
        hosts.add("host2");
        // WHEN
        boolean result = underTest.filterNodes(createNode(), hosts, new HashSet<>(), new HashSet<>());
        // THEN
        assertFalse(result);
    }

    @Test
    public void testFilterNodesWithHostGroupFilter() {
        // GIVEN
        Set<String> hostGroups = new HashSet<>();
        hostGroups.add("master");
        // WHEN
        boolean result = underTest.filterNodes(createNode(), new HashSet<>(), hostGroups, new HashSet<>());
        // THEN
        assertTrue(result);
    }

    @Test
    public void testFilterNodesWithWrongHostGroupFilter() {
        // GIVEN
        Set<String> hostGroups = new HashSet<>();
        hostGroups.add("idbroker");
        // WHEN
        boolean result = underTest.filterNodes(createNode(), new HashSet<>(), hostGroups, new HashSet<>());
        // THEN
        assertFalse(result);
    }

    @Test
    public void testFilterNodesWithHostAndHostGroupFilter() {
        // GIVEN
        Set<String> hostGroups = new HashSet<>();
        hostGroups.add("master");
        Set<String> hosts = new HashSet<>();
        hosts.add("host1");
        // WHEN
        boolean result = underTest.filterNodes(createNode(), hosts, hostGroups, new HashSet<>());
        // THEN
        assertTrue(result);
    }

    @Test
    public void testFilterNodesWithHostAndWrongHostGroupFilter() {
        // GIVEN
        Set<String> hostGroups = new HashSet<>();
        hostGroups.add("idbroker");
        Set<String> hosts = new HashSet<>();
        hosts.add("host1");
        // WHEN
        boolean result = underTest.filterNodes(createNode(), hosts, hostGroups, new HashSet<>());
        // THEN
        assertFalse(result);
    }

    @Test
    public void testFilterNodesWithWrongHostAndHostGroupFilter() {
        // GIVEN
        Set<String> hostGroups = new HashSet<>();
        hostGroups.add("master");
        Set<String> hosts = new HashSet<>();
        hosts.add("host2");
        // WHEN
        boolean result = underTest.filterNodes(createNode(), hosts, hostGroups, new HashSet<>());
        // THEN
        assertFalse(result);
    }

    @Test
    public void testFilterNodesWithEmptyFilters() {
        // GIVEN
        // WHEN
        boolean result = underTest.filterNodes(createNode(), new HashSet<>(), new HashSet<>(), new HashSet<>());
        // THEN
        assertTrue(result);
    }

    @Test
    public void testFilterNodesWithExcludedHosts() {
        // GIVEN
        Set<String> excludedHosts = new HashSet<>();
        excludedHosts.add("host1");
        // WHEN
        boolean result = underTest.filterNodes(createNode(), new HashSet<>(), new HashSet<>(), excludedHosts);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testFilterNodesWithExcludedHostsWithDifferentHost() {
        // GIVEN
        Set<String> excludedHosts = new HashSet<>();
        excludedHosts.add("host2");
        // WHEN
        boolean result = underTest.filterNodes(createNode(), new HashSet<>(), new HashSet<>(), excludedHosts);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testFilterNodesWithExcludedHostsWithPrecedence() {
        // GIVEN
        Set<String> hosts = new HashSet<>();
        hosts.add("host2");
        Set<String> excludedHosts = new HashSet<>();
        excludedHosts.add("host1");
        // WHEN
        boolean result = underTest.filterNodes(createNode(), hosts, new HashSet<>(), excludedHosts);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsVersionGreaterOrEqualIfVersionEquals() {
        // GIVEN
        // WHEN
        boolean result = underTest.isVersionGreaterOrEqual("0.4.8", "0.4.8");
        // THEN
        assertTrue(result);
    }

    @Test
    public void testIsVersionGreaterOrEqualIfVersionLess() {
        // GIVEN
        // WHEN
        boolean result = underTest.isVersionGreaterOrEqual("0.4.7", "0.4.8");
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsVersionGreaterOrEqualIfVersionGreater() {
        // GIVEN
        // WHEN
        boolean result = underTest.isVersionGreaterOrEqual("0.4.9", "0.4.8");
        // THEN
        assertTrue(result);
    }

    private Node createNode() {
        return new Node("privateIp", "publicIp", "instanceId",
                "instanceType", "host1", "master");
    }

}
