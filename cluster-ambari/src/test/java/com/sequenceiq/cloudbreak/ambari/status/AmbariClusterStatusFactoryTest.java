package com.sequenceiq.cloudbreak.ambari.status;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.ambari.AmbariAdapter;
import com.sequenceiq.cloudbreak.ambari.AmbariClientFactory;
import com.sequenceiq.cloudbreak.ambari.AmbariClusterStatusFactory;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterStatusFactoryTest {
    private static final String TEST_BLUEPRINT = "blueprint";

    private static final String TEST_COMP1 = "comp1";

    private static final String TEST_COMP2 = "comp2";

    private static final String TEST_COMP3 = "comp3";

    private static final String TEST_COMP4 = "comp4";

    private static final String TEST_COMP5 = "comp5";

    private static final String TEST_CLIENT_COMP = "clientcomp";

    @InjectMocks
    private AmbariClusterStatusFactory underTest;

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private AmbariClientFactory ambariClientFactory;

    @Spy
    private AmbariAdapter ambariAdapter;

    private HttpClientConfig clientConfig = new HttpClientConfig("1.1.1.1");

    private Stack stack = new Stack();

    private Cluster cluster = new Cluster();

    @Before
    public void setUp() {
        stack.setCluster(cluster);
        when(ambariClientFactory.getAmbariClient(any(), any(), any())).thenReturn(ambariClient);
    }

    @Test
    public void testCreateClusterStatusShouldReturnAmbariServerNotRunningStatusWhenAmbariServerIsNotRunning() throws IOException, URISyntaxException {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willThrow(new RuntimeException());
        // WHEN
        ClusterStatus actualResult = underTest.createClusterStatus(stack, clientConfig, true).getClusterStatus();
        // THEN
        Assert.assertEquals(ClusterStatus.AMBARISERVER_NOT_RUNNING, actualResult);
    }

    @Test
    public void testCreateClusterStatusShouldReturnPendingStatusWhenThereAreInProgressOperations() throws IOException, URISyntaxException {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        BDDMockito.given(ambariClient.getRequests("IN_PROGRESS", "PENDING")).willReturn(Collections.singletonMap("IN_PROGRESS",
                Collections.singletonList(1)));
        // WHEN
        ClusterStatus actualResult = underTest.createClusterStatus(stack, clientConfig, true).getClusterStatus();
        // THEN
        Assert.assertEquals(ClusterStatus.PENDING, actualResult);
    }

    @Test
    public void testCreateClusterStatusShouldReturnAmbariRunningStatusWhenNoBlueprintGiven() throws IOException, URISyntaxException {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        // WHEN
        ClusterStatus actualResult = underTest.createClusterStatus(stack, clientConfig, false).getClusterStatus();
        // THEN
        Assert.assertEquals(ClusterStatus.AMBARISERVER_RUNNING, actualResult);
        Assert.assertEquals(Status.AVAILABLE, actualResult.getStackStatus());
        Assert.assertNull(actualResult.getClusterStatus());
    }

    @Test
    public void testCreateClusterStatusShouldReturnAvailabelStackWithStoppedClusterWhenAllServerComponentsAreInstalled() throws IOException, URISyntaxException {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        BDDMockito.given(ambariClient.getHostComponentsStatesCategorized()).willReturn(createHostComponentsStates("INSTALLED"));
        // WHEN
        ClusterStatus actualResult = underTest.createClusterStatus(stack, clientConfig, true).getClusterStatus();
        // THEN
        Assert.assertEquals(ClusterStatus.INSTALLED, actualResult);
        Assert.assertEquals(Status.AVAILABLE, actualResult.getStackStatus());
        Assert.assertEquals(Status.STOPPED, actualResult.getClusterStatus());
    }

    @Test
    public void testCreateClusterStatusShouldReturnAvailableClusterWhenAllServerComponentsAreStarted() throws IOException, URISyntaxException {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        BDDMockito.given(ambariClient.getHostComponentsStatesCategorized()).willReturn(createHostComponentsStates("STARTED"));
        // WHEN
        ClusterStatus actualResult = underTest.createClusterStatus(stack, clientConfig, true).getClusterStatus();
        // THEN
        Assert.assertEquals(ClusterStatus.STARTED, actualResult);
        Assert.assertEquals(Status.AVAILABLE, actualResult.getStackStatus());
        Assert.assertEquals(Status.AVAILABLE, actualResult.getClusterStatus());
    }

    @Test
    public void testCreateClusterStatusShouldReturnInstallingStatusWhenOneServerComponentIsBeingInstalled() throws IOException, URISyntaxException {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        BDDMockito.given(ambariClient.getHostComponentsStatesCategorized()).willReturn(createInstallingHostComponentsStates());
        // WHEN
        ClusterStatus actualResult = underTest.createClusterStatus(stack, clientConfig, true).getClusterStatus();
        // THEN
        Assert.assertEquals(ClusterStatus.INSTALLING, actualResult);
    }

    @Test
    public void testCreateClusterStatusShouldReturnAmbiguousWhenThereAreStartedAndInstalledComps() throws IOException, URISyntaxException {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        BDDMockito.given(ambariClient.getHostComponentsStatesCategorized()).willReturn(createInstalledAndStartedHostComponentsStates());
        // WHEN
        ClusterStatus actualResult = underTest.createClusterStatus(stack, clientConfig, true).getClusterStatus();
        // THEN
        Assert.assertEquals(ClusterStatus.AMBIGUOUS, actualResult);
    }

    @Test
    public void testCreateClusterStatusShouldReturnAmbiguousStatusWhenThereAreCompsInUnsupportedStates() throws IOException, URISyntaxException {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        BDDMockito.given(ambariClient.getHostComponentsStatesCategorized()).willReturn(createHostComponentsStates("Unsupported"));
        // WHEN
        ClusterStatus actualResult = underTest.createClusterStatus(stack, clientConfig, true).getClusterStatus();
        // THEN
        Assert.assertEquals(ClusterStatus.AMBIGUOUS, actualResult);
    }

    @Test
    public void testCreateClusterStatusShouldReturnUnknownWhenAmbariThrowsException() throws IOException, URISyntaxException {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        BDDMockito.given(ambariClient.getHostComponentsStatesCategorized()).willThrow(new RuntimeException());
        // WHEN
        ClusterStatus actualResult = underTest.createClusterStatus(stack, clientConfig, true).getClusterStatus();
        // THEN
        Assert.assertEquals(ClusterStatus.UNKNOWN, actualResult);
    }

    private Map<String, String> createHostComponentState(String host, String component, String state, String category) {
        Map<String, String> hostComponentState = new HashMap<>();
        hostComponentState.put("host", host);
        hostComponentState.put("component_name", component);
        hostComponentState.put("state", state);
        hostComponentState.put("category", category);
        return hostComponentState;
    }

    private List<Map<String, String>> createHostComponentsStates(String state) {
        List<Map<String, String>> result = new ArrayList<>();
        result.add(createHostComponentState("host1", TEST_COMP1, state, "MASTER"));
        result.add(createHostComponentState("host1", TEST_COMP2, state, "MASTER"));
        result.add(createHostComponentState("host2", TEST_COMP3, state, "SLAVE"));
        result.add(createHostComponentState("host2", TEST_COMP4, state, "SLAVE"));
        result.add(createHostComponentState("host2", TEST_COMP5, state, "SLAVE"));
        result.add(createHostComponentState("host2", TEST_CLIENT_COMP, "NotImportant", "CLIENT"));
        return result;
    }

    private List<Map<String, String>> createInstallingHostComponentsStates() {
        List<Map<String, String>> result = new ArrayList<>();
        result.add(createHostComponentState("host1", TEST_COMP1, "INSTALLED", "MASTER"));
        result.add(createHostComponentState("host1", TEST_COMP2, "INSTALLING", "MASTER"));
        result.add(createHostComponentState("host2", TEST_COMP3, "INSTALL_FAILED", "SLAVE"));
        result.add(createHostComponentState("host2", TEST_COMP4, "STARTING", "SLAVE"));
        result.add(createHostComponentState("host2", TEST_COMP5, "STARTED", "SLAVE"));
        result.add(createHostComponentState("host2", TEST_CLIENT_COMP, "NotImportant", "CLIENT"));
        return result;
    }

    private List<Map<String, String>> createInstalledAndStartedHostComponentsStates() {
        List<Map<String, String>> result = new ArrayList<>();
        result.add(createHostComponentState("host1", TEST_COMP1, "INSTALLED", "MASTER"));
        result.add(createHostComponentState("host1", TEST_COMP2, "STARTED", "MASTER"));
        result.add(createHostComponentState("host2", TEST_COMP3, "INSTALLED", "SLAVE"));
        result.add(createHostComponentState("host2", TEST_COMP4, "STARTED", "SLAVE"));
        result.add(createHostComponentState("host2", TEST_COMP5, "STARTED", "SLAVE"));
        result.add(createHostComponentState("host2", TEST_CLIENT_COMP, "NotImportant", "CLIENT"));
        return result;
    }
}
