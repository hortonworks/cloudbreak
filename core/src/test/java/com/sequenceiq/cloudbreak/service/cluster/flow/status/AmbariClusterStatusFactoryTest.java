package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Status;

public class AmbariClusterStatusFactoryTest {
    private static final String TEST_BLUEPRINT = "blueprint";
    private static final String TEST_COMP1 = "comp1";
    private static final String TEST_COMP2 = "comp2";
    private static final String TEST_COMP3 = "comp3";
    private static final String TEST_COMP4 = "comp4";
    private static final String TEST_COMP5 = "comp5";
    private static final String TEST_CLIENT_COMP = "clientcomp";

    private AmbariClusterStatusFactory underTest;

    @Mock
    private AmbariClient ambariClient;

    @Before
    public void setUp() {
        underTest = new AmbariClusterStatusFactory();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateClusterStatusShouldReturnNullWhenAmbariServerIsNotRunning() {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willThrow(new RuntimeException());
        // WHEN
        AmbariClusterStatus actualResult = underTest.createClusterStatus(ambariClient, TEST_BLUEPRINT);
        // THEN
        Assert.assertNull(actualResult);
    }

    @Test
    public void testCreateClusterStatusShouldReturnAmbariRunningStatusWhenNoBlueprintGiven() {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        // WHEN
        AmbariClusterStatus actualResult = underTest.createClusterStatus(ambariClient, null);
        // THEN
        Assert.assertEquals(ClusterStatus.AMBARISERVER_RUNNING, actualResult.getStatus());
        Assert.assertEquals(Status.AVAILABLE, actualResult.getStackStatus());
        Assert.assertNull(actualResult.getClusterStatus());
    }

    @Test
    public void testCreateClusterStatusShouldReturnAvailabelStackWithStoppedClusterWhenAllServerComponentsAreInstalled() {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        BDDMockito.given(ambariClient.getComponentsCategory(TEST_BLUEPRINT)).willReturn(createComponentCategories());
        BDDMockito.given(ambariClient.getHostComponentsStates()).willReturn(createHostComponentsStates("INSTALLED"));
        // WHEN
        AmbariClusterStatus actualResult = underTest.createClusterStatus(ambariClient, TEST_BLUEPRINT);
        // THEN
        Assert.assertEquals(ClusterStatus.INSTALLED, actualResult.getStatus());
        Assert.assertEquals(Status.AVAILABLE, actualResult.getStackStatus());
        Assert.assertEquals(Status.STOPPED, actualResult.getClusterStatus());
    }

    @Test
    public void testCreateClusterStatusShouldReturnAvailableClusterWhenAllServerComponentsAreStarted() {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        BDDMockito.given(ambariClient.getComponentsCategory(TEST_BLUEPRINT)).willReturn(createComponentCategories());
        BDDMockito.given(ambariClient.getHostComponentsStates()).willReturn(createHostComponentsStates("STARTED"));
        // WHEN
        AmbariClusterStatus actualResult = underTest.createClusterStatus(ambariClient, TEST_BLUEPRINT);
        // THEN
        Assert.assertEquals(ClusterStatus.STARTED, actualResult.getStatus());
        Assert.assertEquals(Status.AVAILABLE, actualResult.getStackStatus());
        Assert.assertEquals(Status.AVAILABLE, actualResult.getClusterStatus());
    }

    @Test
    public void testCreateClusterStatusShouldReturnNullWhenOneServerComponentIsBeingInstalled() {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        BDDMockito.given(ambariClient.getComponentsCategory(TEST_BLUEPRINT)).willReturn(createComponentCategories());
        BDDMockito.given(ambariClient.getHostComponentsStates()).willReturn(createInstallingHostComponentsStates());
        // WHEN
        AmbariClusterStatus actualResult = underTest.createClusterStatus(ambariClient, TEST_BLUEPRINT);
        // THEN
        Assert.assertNull(actualResult);
    }

    @Test
    public void testCreateClusterStatusShouldReturnNullWhenThereAreStartedAndInstalledComps() {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        BDDMockito.given(ambariClient.getComponentsCategory(TEST_BLUEPRINT)).willReturn(createComponentCategories());
        BDDMockito.given(ambariClient.getHostComponentsStates()).willReturn(createInstalledAndStartedHostComponentsStates());
        // WHEN
        AmbariClusterStatus actualResult = underTest.createClusterStatus(ambariClient, TEST_BLUEPRINT);
        // THEN
        Assert.assertNull(actualResult);
    }

    @Test
    public void testCreateClusterStatusShouldReturnNullWhenThereAreCompsInUnsupportedStates() {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        BDDMockito.given(ambariClient.getComponentsCategory(TEST_BLUEPRINT)).willReturn(createComponentCategories());
        BDDMockito.given(ambariClient.getHostComponentsStates()).willReturn(createHostComponentsStates("Unsupported"));
        // WHEN
        AmbariClusterStatus actualResult = underTest.createClusterStatus(ambariClient, TEST_BLUEPRINT);
        // THEN
        Assert.assertNull(actualResult);
    }

    @Test
    public void testCreateClusterStatusShouldReturnNullWhenAmbariThrowsException() {
        // GIVEN
        BDDMockito.given(ambariClient.healthCheck()).willReturn("RUNNING");
        BDDMockito.given(ambariClient.getComponentsCategory(TEST_BLUEPRINT)).willThrow(new RuntimeException());
        // WHEN
        AmbariClusterStatus actualResult = underTest.createClusterStatus(ambariClient, TEST_BLUEPRINT);
        // THEN
        Assert.assertNull(actualResult);
    }

    private Map<String, String> createComponentCategories() {
        Map<String, String> categoryMap = new HashMap<>();
        categoryMap.put(TEST_COMP1, "MASTER");
        categoryMap.put(TEST_COMP2, "MASTER");
        categoryMap.put(TEST_COMP3, "SLAVE");
        categoryMap.put(TEST_COMP4, "SLAVE");
        categoryMap.put(TEST_COMP5, "SLAVE");
        categoryMap.put(TEST_CLIENT_COMP, "CLIENT");
        return categoryMap;
    }

    private Map<String, Map<String, String>> createHostComponentsStates(String state) {
        Map<String, Map<String, String>> result = new HashMap<>();
        Map<String, String> host1ComponentsStates = new HashMap<>();
        host1ComponentsStates.put(TEST_COMP1, state);
        host1ComponentsStates.put(TEST_COMP2, state);
        result.put("host1", host1ComponentsStates);
        Map<String, String> host2ComponentsStates = new HashMap<>();
        host2ComponentsStates.put(TEST_COMP3, state);
        host2ComponentsStates.put(TEST_COMP4, state);
        host2ComponentsStates.put(TEST_COMP5, state);
        host2ComponentsStates.put(TEST_CLIENT_COMP, "NotImportant");
        result.put("host2", host2ComponentsStates);
        return result;
    }

    private Map<String, Map<String, String>> createInstallingHostComponentsStates() {
        Map<String, Map<String, String>> result = new HashMap<>();
        Map<String, String> host1ComponentsStates = new HashMap<>();
        host1ComponentsStates.put(TEST_COMP1, "INSTALLED");
        host1ComponentsStates.put(TEST_COMP2, "INSTALLING");
        result.put("host1", host1ComponentsStates);
        Map<String, String> host2ComponentsStates = new HashMap<>();
        host2ComponentsStates.put(TEST_COMP3, "INSTALL_FAILED");
        host2ComponentsStates.put(TEST_COMP4, "STARTING");
        host2ComponentsStates.put(TEST_COMP5, "STARTED");
        host2ComponentsStates.put(TEST_CLIENT_COMP, "NotImportant");
        result.put("host2", host2ComponentsStates);
        return result;
    }

    private Map<String, Map<String, String>> createInstalledAndStartedHostComponentsStates() {
        Map<String, Map<String, String>> result = new HashMap<>();
        Map<String, String> host1ComponentsStates = new HashMap<>();
        host1ComponentsStates.put(TEST_COMP1, "INSTALLED");
        host1ComponentsStates.put(TEST_COMP2, "STARTED");
        result.put("host1", host1ComponentsStates);
        Map<String, String> host2ComponentsStates = new HashMap<>();
        host2ComponentsStates.put(TEST_COMP3, "INSTALLED");
        host2ComponentsStates.put(TEST_COMP4, "STARTED");
        host2ComponentsStates.put(TEST_COMP5, "STARTED");
        host2ComponentsStates.put(TEST_CLIENT_COMP, "NotImportant");
        result.put("host2", host2ComponentsStates);
        return result;
    }
}
