package com.sequenceiq.cloudbreak.ambari;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.ambari.AmbariAdapter.ClusterStatusResult;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;

@RunWith(MockitoJUnitRunner.class)
public class AmbariAdapterTest {

    private static final String TEST_COMP1 = "comp1";

    private static final String TEST_COMP2 = "comp2";

    private static final String TEST_COMP3 = "comp3";

    private static final String TEST_COMP4 = "comp4";

    private static final String TEST_COMP5 = "comp5";

    private static final String TEST_CLIENT_COMP = "clientcomp";

    @InjectMocks
    private AmbariAdapter underTest;

    @Mock
    private AmbariClient ambariClient;

    @Test
    public void testCreateClusterStatusShouldReturnAvailabelStackWithStoppedClusterWhenAllServerComponentsAreInstalled() {
        BDDMockito.given(ambariClient.getHostComponentsStatesCategorized()).willReturn(createHostComponentsStates("INSTALLED"));
        ClusterStatusResult actualResult = underTest.getClusterStatusHostComponentMap(ambariClient);
        Assert.assertEquals(ClusterStatus.INSTALLED, actualResult.getClusterStatus());
    }

    @Test
    public void testCreateClusterStatusShouldReturnAvailableClusterWhenAllServerComponentsAreStarted() {
        BDDMockito.given(ambariClient.getHostComponentsStatesCategorized()).willReturn(createHostComponentsStates("STARTED"));
        ClusterStatusResult actualResult = underTest.getClusterStatusHostComponentMap(ambariClient);
        Assert.assertEquals(ClusterStatus.STARTED, actualResult.getClusterStatus());
    }

    @Test
    public void testCreateClusterStatusShouldReturnInstallingStatusWhenOneServerComponentIsBeingInstalled() {
        BDDMockito.given(ambariClient.getHostComponentsStatesCategorized()).willReturn(createInstallingHostComponentsStates());
        ClusterStatusResult actualResult = underTest.getClusterStatusHostComponentMap(ambariClient);
        Assert.assertEquals(ClusterStatus.INSTALLING, actualResult.getClusterStatus());
    }

    @Test
    public void testCreateClusterStatusShouldReturnAmbiguousWhenThereAreStartedAndInstalledComps() {
        BDDMockito.given(ambariClient.getHostComponentsStatesCategorized()).willReturn(createInstalledAndStartedHostComponentsStates());
        ClusterStatusResult actualResult = underTest.getClusterStatusHostComponentMap(ambariClient);
        Assert.assertEquals(ClusterStatus.AMBIGUOUS, actualResult.getClusterStatus());
    }

    @Test
    public void testCreateClusterStatusShouldReturnAmbiguousStatusWhenThereAreCompsInUnsupportedStates() {
        BDDMockito.given(ambariClient.getHostComponentsStatesCategorized()).willReturn(createHostComponentsStates("Unsupported"));
        ClusterStatusResult actualResult = underTest.getClusterStatusHostComponentMap(ambariClient);
        Assert.assertEquals(ClusterStatus.AMBIGUOUS, actualResult.getClusterStatus());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateClusterStatusShouldReturnUnknownWhenAmbariThrowsException() {
        BDDMockito.given(ambariClient.getHostComponentsStatesCategorized()).willThrow(new RuntimeException());
        ClusterStatusResult actualResult = underTest.getClusterStatusHostComponentMap(ambariClient);
        Assert.assertEquals(ClusterStatus.UNKNOWN, actualResult.getClusterStatus());
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
        result.add(createHostComponentState("host2", TEST_COMP5, "NotImportant", "SLAVE"));
        result.add(createHostComponentState("host2", TEST_CLIENT_COMP, "NotImportant", "CLIENT"));
        return result;
    }
}