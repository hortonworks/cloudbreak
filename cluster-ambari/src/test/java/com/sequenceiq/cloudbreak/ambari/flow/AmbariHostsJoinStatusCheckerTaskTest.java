package com.sequenceiq.cloudbreak.ambari.flow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;

public class AmbariHostsJoinStatusCheckerTaskTest {

    private final AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

    private final AmbariHostsJoinStatusCheckerTask underTest = new AmbariHostsJoinStatusCheckerTask();

    @Test
    public void testWithAllJoined() {
        Set<HostMetadata> hostsInCluster = AmbariHostCheckerTestUtils.getHostMetadataSet(3);
        AmbariHostsCheckerContext ambariHostsCheckerContext = new AmbariHostsCheckerContext(new Stack(), ambariClient, hostsInCluster, 0);
        Map<String, String> hostStates = AmbariHostCheckerTestUtils.getHostStatuses("UNHEALTHY", "HEALTHY", "HEALTHY");
        when(ambariClient.getHostStatuses()).thenReturn(hostStates);
        boolean result = underTest.checkStatus(ambariHostsCheckerContext);
        assertTrue(result);
    }

    @Test
    public void testWithOneUnknown() {
        Set<HostMetadata> hostsInCluster = AmbariHostCheckerTestUtils.getHostMetadataSet(3);
        AmbariHostsCheckerContext ambariHostsCheckerContext = new AmbariHostsCheckerContext(new Stack(), ambariClient, hostsInCluster, 0);
        Map<String, String> hostStates = AmbariHostCheckerTestUtils.getHostStatuses("UNKNOWN", "HEALTHY", "HEALTHY");
        when(ambariClient.getHostStatuses()).thenReturn(hostStates);
        boolean result = underTest.checkStatus(ambariHostsCheckerContext);
        assertFalse(result);
    }

    @Test
    public void testWithTwoUnknowns() {
        Set<HostMetadata> hostsInCluster = AmbariHostCheckerTestUtils.getHostMetadataSet(4);
        AmbariHostsCheckerContext ambariHostsCheckerContext = new AmbariHostsCheckerContext(new Stack(), ambariClient, hostsInCluster, 0);
        Map<String, String> hostStates = AmbariHostCheckerTestUtils.getHostStatuses("UNKNOWN", "HEALTHY", "HEALTHY", "UNKNOWN");
        when(ambariClient.getHostStatuses()).thenReturn(hostStates);
        boolean result = underTest.checkStatus(ambariHostsCheckerContext);
        assertFalse(result);
    }

    @Test
    public void testWithMissingUnknownHostMetadata() {
        Set<HostMetadata> hostsInCluster = AmbariHostCheckerTestUtils.getHostMetadataSet(2);
        AmbariHostsCheckerContext ambariHostsCheckerContext = new AmbariHostsCheckerContext(new Stack(), ambariClient, hostsInCluster, 0);
        Map<String, String> hostStates = AmbariHostCheckerTestUtils.getHostStatuses("HEALTHY", "HEALTHY", "UNKNOWN");
        when(ambariClient.getHostStatuses()).thenReturn(hostStates);
        boolean result = underTest.checkStatus(ambariHostsCheckerContext);
        assertTrue(result);
    }

    @Test
    public void testWithMissingHostStatus() {
        Set<HostMetadata> hostsInCluster = AmbariHostCheckerTestUtils.getHostMetadataSet(3);
        AmbariHostsCheckerContext ambariHostsCheckerContext = new AmbariHostsCheckerContext(new Stack(), ambariClient, hostsInCluster, 0);
        Map<String, String> hostStates = AmbariHostCheckerTestUtils.getHostStatuses("HEALTHY", "HEALTHY");
        when(ambariClient.getHostStatuses()).thenReturn(hostStates);
        boolean result = underTest.checkStatus(ambariHostsCheckerContext);
        assertFalse(result);
    }

}