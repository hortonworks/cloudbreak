package com.sequenceiq.cloudbreak.service.cluster.flow.status

import java.util.Collections
import java.util.HashMap

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.api.model.Status

class AmbariClusterStatusFactoryTest {

    private var underTest: AmbariClusterStatusFactory? = null

    @Mock
    private val ambariClient: AmbariClient? = null

    @Before
    fun setUp() {
        underTest = AmbariClusterStatusFactory()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testCreateClusterStatusShouldReturnAmbariServerNotRunningStatusWhenAmbariServerIsNotRunning() {
        // GIVEN
        BDDMockito.given(ambariClient!!.healthCheck()).willThrow(RuntimeException())
        // WHEN
        val actualResult = underTest!!.createClusterStatus(ambariClient, TEST_BLUEPRINT)
        // THEN
        Assert.assertEquals(ClusterStatus.AMBARISERVER_NOT_RUNNING, actualResult)
    }

    @Test
    fun testCreateClusterStatusShouldReturnPendingStatusWhenThereAreInProgressOperations() {
        // GIVEN
        BDDMockito.given(ambariClient!!.healthCheck()).willReturn("RUNNING")
        BDDMockito.given(ambariClient.getRequests("IN_PROGRESS", "PENDING")).willReturn(Collections.singletonMap<String, List<Int>>("IN_PROGRESS",
                listOf<Int>(1)))
        // WHEN
        val actualResult = underTest!!.createClusterStatus(ambariClient, TEST_BLUEPRINT)
        // THEN
        Assert.assertEquals(ClusterStatus.PENDING, actualResult)
    }

    @Test
    fun testCreateClusterStatusShouldReturnAmbariRunningStatusWhenNoBlueprintGiven() {
        // GIVEN
        BDDMockito.given(ambariClient!!.healthCheck()).willReturn("RUNNING")
        // WHEN
        val actualResult = underTest!!.createClusterStatus(ambariClient, null)
        // THEN
        Assert.assertEquals(ClusterStatus.AMBARISERVER_RUNNING, actualResult)
        Assert.assertEquals(Status.AVAILABLE, actualResult.stackStatus)
        Assert.assertNull(actualResult.clusterStatus)
    }

    @Test
    fun testCreateClusterStatusShouldReturnAvailabelStackWithStoppedClusterWhenAllServerComponentsAreInstalled() {
        // GIVEN
        BDDMockito.given(ambariClient!!.healthCheck()).willReturn("RUNNING")
        BDDMockito.given(ambariClient.getComponentsCategory(TEST_BLUEPRINT)).willReturn(createComponentCategories())
        BDDMockito.given(ambariClient.hostComponentsStates).willReturn(createHostComponentsStates("INSTALLED"))
        // WHEN
        val actualResult = underTest!!.createClusterStatus(ambariClient, TEST_BLUEPRINT)
        // THEN
        Assert.assertEquals(ClusterStatus.INSTALLED, actualResult)
        Assert.assertEquals(Status.AVAILABLE, actualResult.stackStatus)
        Assert.assertEquals(Status.STOPPED, actualResult.clusterStatus)
    }

    @Test
    fun testCreateClusterStatusShouldReturnAvailableClusterWhenAllServerComponentsAreStarted() {
        // GIVEN
        BDDMockito.given(ambariClient!!.healthCheck()).willReturn("RUNNING")
        BDDMockito.given(ambariClient.getComponentsCategory(TEST_BLUEPRINT)).willReturn(createComponentCategories())
        BDDMockito.given(ambariClient.hostComponentsStates).willReturn(createHostComponentsStates("STARTED"))
        // WHEN
        val actualResult = underTest!!.createClusterStatus(ambariClient, TEST_BLUEPRINT)
        // THEN
        Assert.assertEquals(ClusterStatus.STARTED, actualResult)
        Assert.assertEquals(Status.AVAILABLE, actualResult.stackStatus)
        Assert.assertEquals(Status.AVAILABLE, actualResult.clusterStatus)
    }

    @Test
    fun testCreateClusterStatusShouldReturnInstallingStatusWhenOneServerComponentIsBeingInstalled() {
        // GIVEN
        BDDMockito.given(ambariClient!!.healthCheck()).willReturn("RUNNING")
        BDDMockito.given(ambariClient.getComponentsCategory(TEST_BLUEPRINT)).willReturn(createComponentCategories())
        BDDMockito.given(ambariClient.hostComponentsStates).willReturn(createInstallingHostComponentsStates())
        // WHEN
        val actualResult = underTest!!.createClusterStatus(ambariClient, TEST_BLUEPRINT)
        // THEN
        Assert.assertEquals(ClusterStatus.INSTALLING, actualResult)
    }

    @Test
    fun testCreateClusterStatusShouldReturnAmbiguousWhenThereAreStartedAndInstalledComps() {
        // GIVEN
        BDDMockito.given(ambariClient!!.healthCheck()).willReturn("RUNNING")
        BDDMockito.given(ambariClient.getComponentsCategory(TEST_BLUEPRINT)).willReturn(createComponentCategories())
        BDDMockito.given(ambariClient.hostComponentsStates).willReturn(createInstalledAndStartedHostComponentsStates())
        // WHEN
        val actualResult = underTest!!.createClusterStatus(ambariClient, TEST_BLUEPRINT)
        // THEN
        Assert.assertEquals(ClusterStatus.AMBIGUOUS, actualResult)
    }

    @Test
    fun testCreateClusterStatusShouldReturnAmbiguousStatusWhenThereAreCompsInUnsupportedStates() {
        // GIVEN
        BDDMockito.given(ambariClient!!.healthCheck()).willReturn("RUNNING")
        BDDMockito.given(ambariClient.getComponentsCategory(TEST_BLUEPRINT)).willReturn(createComponentCategories())
        BDDMockito.given(ambariClient.hostComponentsStates).willReturn(createHostComponentsStates("Unsupported"))
        // WHEN
        val actualResult = underTest!!.createClusterStatus(ambariClient, TEST_BLUEPRINT)
        // THEN
        Assert.assertEquals(ClusterStatus.AMBIGUOUS, actualResult)
    }

    @Test
    fun testCreateClusterStatusShouldReturnUnknownWhenAmbariThrowsException() {
        // GIVEN
        BDDMockito.given(ambariClient!!.healthCheck()).willReturn("RUNNING")
        BDDMockito.given(ambariClient.getComponentsCategory(TEST_BLUEPRINT)).willThrow(RuntimeException())
        // WHEN
        val actualResult = underTest!!.createClusterStatus(ambariClient, TEST_BLUEPRINT)
        // THEN
        Assert.assertEquals(ClusterStatus.UNKNOWN, actualResult)
    }

    private fun createComponentCategories(): Map<String, String> {
        val categoryMap = HashMap<String, String>()
        categoryMap.put(TEST_COMP1, "MASTER")
        categoryMap.put(TEST_COMP2, "MASTER")
        categoryMap.put(TEST_COMP3, "SLAVE")
        categoryMap.put(TEST_COMP4, "SLAVE")
        categoryMap.put(TEST_COMP5, "SLAVE")
        categoryMap.put(TEST_CLIENT_COMP, "CLIENT")
        return categoryMap
    }

    private fun createHostComponentsStates(state: String): Map<String, Map<String, String>> {
        val result = HashMap<String, Map<String, String>>()
        val host1ComponentsStates = HashMap<String, String>()
        host1ComponentsStates.put(TEST_COMP1, state)
        host1ComponentsStates.put(TEST_COMP2, state)
        result.put("host1", host1ComponentsStates)
        val host2ComponentsStates = HashMap<String, String>()
        host2ComponentsStates.put(TEST_COMP3, state)
        host2ComponentsStates.put(TEST_COMP4, state)
        host2ComponentsStates.put(TEST_COMP5, state)
        host2ComponentsStates.put(TEST_CLIENT_COMP, "NotImportant")
        result.put("host2", host2ComponentsStates)
        return result
    }

    private fun createInstallingHostComponentsStates(): Map<String, Map<String, String>> {
        val result = HashMap<String, Map<String, String>>()
        val host1ComponentsStates = HashMap<String, String>()
        host1ComponentsStates.put(TEST_COMP1, "INSTALLED")
        host1ComponentsStates.put(TEST_COMP2, "INSTALLING")
        result.put("host1", host1ComponentsStates)
        val host2ComponentsStates = HashMap<String, String>()
        host2ComponentsStates.put(TEST_COMP3, "INSTALL_FAILED")
        host2ComponentsStates.put(TEST_COMP4, "STARTING")
        host2ComponentsStates.put(TEST_COMP5, "STARTED")
        host2ComponentsStates.put(TEST_CLIENT_COMP, "NotImportant")
        result.put("host2", host2ComponentsStates)
        return result
    }

    private fun createInstalledAndStartedHostComponentsStates(): Map<String, Map<String, String>> {
        val result = HashMap<String, Map<String, String>>()
        val host1ComponentsStates = HashMap<String, String>()
        host1ComponentsStates.put(TEST_COMP1, "INSTALLED")
        host1ComponentsStates.put(TEST_COMP2, "STARTED")
        result.put("host1", host1ComponentsStates)
        val host2ComponentsStates = HashMap<String, String>()
        host2ComponentsStates.put(TEST_COMP3, "INSTALLED")
        host2ComponentsStates.put(TEST_COMP4, "STARTED")
        host2ComponentsStates.put(TEST_COMP5, "STARTED")
        host2ComponentsStates.put(TEST_CLIENT_COMP, "NotImportant")
        result.put("host2", host2ComponentsStates)
        return result
    }

    companion object {
        private val TEST_BLUEPRINT = "blueprint"
        private val TEST_COMP1 = "comp1"
        private val TEST_COMP2 = "comp2"
        private val TEST_COMP3 = "comp3"
        private val TEST_COMP4 = "comp4"
        private val TEST_COMP5 = "comp5"
        private val TEST_CLIENT_COMP = "clientcomp"
    }
}
