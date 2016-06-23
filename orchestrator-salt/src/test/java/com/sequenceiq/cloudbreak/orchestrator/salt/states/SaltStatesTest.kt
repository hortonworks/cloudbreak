package com.sequenceiq.cloudbreak.orchestrator.salt.states

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL_ASYNC
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.RUNNER
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.mockito.Matchers.any
import org.mockito.Matchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.io.InputStream
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.stream.Collectors

import org.apache.commons.io.IOUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.runners.MockitoJUnitRunner

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Multimap
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.NetworkInterfaceResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PingResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunningJobsResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType

@RunWith(MockitoJUnitRunner::class)
class SaltStatesTest {

    private var saltConnector: SaltConnector? = null
    private var target: Target<String>? = null

    @Captor
    private val minionIdsCaptor: ArgumentCaptor<List<String>>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val targets = HashSet<String>()
        targets.add("10-0-0-1.example.com")
        targets.add("10-0-0-2.example.com")
        targets.add("10-0-0-3.example.com")
        target = Compound(targets)
        saltConnector = mock<SaltConnector>(SaltConnector::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun pingTest() {
        SaltStates.ping(saltConnector, target)
        verify<SaltConnector>(saltConnector, times(1)).run(eq<Target<String>>(target), eq("test.ping"), eq(LOCAL), eq<Class<PingResponse>>(PingResponse::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun ambariServerTest() {
        val jobId = "1"
        val applyResponse = createApplyResponse(jobId)
        `when`(saltConnector!!.run<ApplyResponse>(target, "state.apply", LOCAL_ASYNC, ApplyResponse::class.java, "ambari.server")).thenReturn(applyResponse)
        val jid = SaltStates.ambariServer(saltConnector, target)
        assertEquals(jobId, jid)
        verify<SaltConnector>(saltConnector, times(1)).run(eq<Target<String>>(target), eq("state.apply"), eq(LOCAL_ASYNC), eq<Class<ApplyResponse>>(ApplyResponse::class.java), eq("ambari.server"))
    }

    @Test
    @Throws(Exception::class)
    fun ambariAgentTest() {
        val jobId = "2"
        val applyResponse = createApplyResponse(jobId)
        `when`(saltConnector!!.run<ApplyResponse>(target, "state.apply", LOCAL_ASYNC, ApplyResponse::class.java, "ambari.agent")).thenReturn(applyResponse)
        val jid = SaltStates.ambariAgent(saltConnector, target)
        assertEquals(jobId, jid)
        verify<SaltConnector>(saltConnector, times(1)).run(eq<Target<String>>(target), eq("state.apply"), eq(LOCAL_ASYNC), eq<Class<ApplyResponse>>(ApplyResponse::class.java), eq("ambari.agent"))
    }

    @Test
    @Throws(Exception::class)
    fun kerberosTest() {
        val jobId = "3"
        val applyResponse = createApplyResponse(jobId)
        `when`(saltConnector!!.run<ApplyResponse>(target, "state.apply", LOCAL_ASYNC, ApplyResponse::class.java, "kerberos.server")).thenReturn(applyResponse)
        val jid = SaltStates.kerberos(saltConnector, target)
        assertEquals(jobId, jid)
        verify<SaltConnector>(saltConnector, times(1)).run(eq<Target<String>>(target), eq("state.apply"), eq(LOCAL_ASYNC), eq<Class<ApplyResponse>>(ApplyResponse::class.java), eq("kerberos.server"))
    }

    private fun createApplyResponse(jobId: String): ApplyResponse {
        val applyResponse = ApplyResponse()
        val result = ArrayList<Map<String, Any>>()
        val resultMap = HashMap<String, Any>()
        resultMap.put("jid", jobId)
        result.add(resultMap)
        applyResponse.result = result
        return applyResponse
    }

    @Test
    @Throws(Exception::class)
    fun addRoleTest() {
        val role = "ambari-server"
        SaltStates.addGrain(saltConnector, target, "roles", role)
        verify<SaltConnector>(saltConnector, times(1)).run(eq<Target<String>>(target), eq("grains.append"), eq(LOCAL), eq<Class<ApplyResponse>>(ApplyResponse::class.java), eq("roles"), eq(role))
    }

    @Test
    @Throws(Exception::class)
    fun syncGrainsTest() {
        SaltStates.syncGrains(saltConnector, target)
        verify<SaltConnector>(saltConnector, times(1)).run(eq(Glob.ALL), eq("saltutil.sync_grains"), eq(LOCAL), eq<Class<ApplyResponse>>(ApplyResponse::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun highstateTest() {
        val jobId = "1"
        val response = ApplyResponse()
        val result = ArrayList<Map<String, Any>>()
        val resultMap = HashMap<String, Any>()
        resultMap.put("jid", jobId)
        result.add(resultMap)
        response.result = result
        `when`(saltConnector!!.run(any<Target<String>>(), eq("state.highstate"), any<SaltClientType>(), eq<Class<ApplyResponse>>(ApplyResponse::class.java))).thenReturn(response)

        val jid = SaltStates.highstate(saltConnector)
        assertEquals(jobId, jid)
        verify<SaltConnector>(saltConnector, times(1)).run(eq(Glob.ALL), eq("state.highstate"), eq(LOCAL_ASYNC), eq<Class<ApplyResponse>>(ApplyResponse::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun jidInfoHighTest() {
        val jobId = "2"

        val responseStream = SaltStatesTest::class.java!!.getResourceAsStream("/jid_response.json")
        val response = IOUtils.toString(responseStream)
        val responseMap = ObjectMapper().readValue<Map<Any, Any>>(response, Map<Any, Any>::class.java)

        `when`(saltConnector!!.run(any<Target<String>>(), eq("jobs.lookup_jid"), any<SaltClientType>(), any<Class<Any>>(), eq("jid"), any<String>())).thenReturn(responseMap)

        val jidInfo = SaltStates.jidInfo(saltConnector, jobId, target, StateType.HIGH)
        verify<SaltConnector>(saltConnector, times(1)).run<Map<Any, Any>>(target, "jobs.lookup_jid", RUNNER, Map<Any, Any>::class.java, "jid", jobId)

        assertThat(jidInfo.keySet(), hasSize<String>(1))
        assertThat(jidInfo.entries(), hasSize<Entry<String, String>>(3))
        val hostName = jidInfo.keySet().iterator().next()
        val hostErrors = jidInfo.get(hostName)

        assertThat(hostErrors, containsInAnyOrder("Source file salt://ambari/scripts/ambari-server-initttt.sh not found",
                "Service ambari-server is already enabled, and is dead",
                "Package haveged is already installed."))
    }

    @Test
    @Throws(Exception::class)
    fun jidInfoSimpleTest() {
        val jobId = "2"

        val responseStream = SaltStatesTest::class.java!!.getResourceAsStream("/jid_simple_response.json")
        val response = IOUtils.toString(responseStream)
        val responseMap = ObjectMapper().readValue<Map<Any, Any>>(response, Map<Any, Any>::class.java)

        `when`(saltConnector!!.run(any<Target<String>>(), eq("jobs.lookup_jid"), any<SaltClientType>(), any<Class<Any>>(), eq("jid"), any<String>())).thenReturn(responseMap)

        val jidInfo = SaltStates.jidInfo(saltConnector, jobId, target, StateType.SIMPLE)
        verify<SaltConnector>(saltConnector, times(1)).run<Map<Any, Any>>(target, "jobs.lookup_jid", RUNNER, Map<Any, Any>::class.java, "jid", jobId)

        assertThat(jidInfo.keySet(), hasSize<String>(1))
        assertThat(jidInfo.entries(), hasSize<Entry<String, String>>(3))
        val hostName = jidInfo.keySet().iterator().next()
        val hostErrors = jidInfo.get(hostName)

        assertThat(hostErrors, containsInAnyOrder("Source file salt://ambari/scripts/ambari-server-initttt.sh not found",
                "Service ambari-server is already enabled, and is dead",
                "Package haveged is already installed."))
    }

    @Test
    @Throws(Exception::class)
    fun jobIsRunningTest() {
        val jid = "3"
        val runningJobsResponse = RunningJobsResponse()
        val result = ArrayList<Map<String, Map<String, Any>>>()
        val resultMap = HashMap<String, Map<String, Any>>()
        resultMap.put(jid, HashMap<String, Any>())
        result.add(resultMap)
        runningJobsResponse.result = result
        `when`(saltConnector!!.run(eq<Target<String>>(target), eq("jobs.active"), any<SaltClientType>(), eq<Class<RunningJobsResponse>>(RunningJobsResponse::class.java), eq("jid"), any<String>())).thenReturn(runningJobsResponse)
        var running = SaltStates.jobIsRunning(saltConnector, jid, target)
        assertEquals(true, running)

        resultMap.clear()
        running = SaltStates.jobIsRunning(saltConnector, jid, target)
        assertEquals(false, running)
    }

    @Test
    @Throws(Exception::class)
    fun networkInterfaceIPTest() {
        SaltStates.networkInterfaceIP(saltConnector, target)
        verify<SaltConnector>(saltConnector, times(1)).run(any<Target<String>>(), eq("network.interface_ip"), eq(LOCAL), eq<Class<NetworkInterfaceResponse>>(NetworkInterfaceResponse::class.java), eq("eth0"))
    }

    @Test
    @Throws(Exception::class)
    fun removeMinionsTest() {
        val hostNames = ArrayList<String>()
        hostNames.add("10-0-0-1.example.com")
        hostNames.add("10-0-0-2.example.com")
        hostNames.add("10-0-0-3.example.com")
        val networkInterfaceResponse = NetworkInterfaceResponse()
        val result = ArrayList<Map<String, String>>()
        val resultMap = HashMap<String, String>()
        resultMap.put("10-0-0-1.example.com", "10.0.0.1")
        resultMap.put("10-0-0-2.example.com", "10.0.0.2")
        resultMap.put("10-0-0-3.example.com", "10.0.0.3")
        result.add(resultMap)
        networkInterfaceResponse.setResult(result)
        `when`(saltConnector!!.run(any<Target<String>>(), eq("network.interface_ip"), eq(LOCAL), eq<Class<NetworkInterfaceResponse>>(NetworkInterfaceResponse::class.java), eq("eth0"))).thenReturn(networkInterfaceResponse)
        SaltStates.removeMinions(saltConnector, hostNames)

        val saltActionArgumentCaptor = ArgumentCaptor.forClass<SaltAction>(SaltAction::class.java)
        verify<SaltConnector>(saltConnector, times(1)).action(saltActionArgumentCaptor.capture())

        val saltAction = saltActionArgumentCaptor.value
        assertEquals(SaltActionType.STOP, saltAction.action)

        assertThat(saltAction.minions, hasSize<Minion>(3))

        val minionAddressList = saltAction.minions.stream().map(Function<Minion, String> { it.getAddress() }).collect(Collectors.toList<String>())
        assertThat<List<String>>(minionAddressList, containsInAnyOrder("10.0.0.1", "10.0.0.2", "10.0.0.3"))

        verify<SaltConnector>(saltConnector, times(1)).wheel(eq("key.delete"), minionIdsCaptor!!.capture(), any<Class<Any>>())
        val minionIds = minionIdsCaptor.value
        assertThat(minionIds, containsInAnyOrder("10-0-0-1.example.com", "10-0-0-2.example.com", "10-0-0-3.example.com"))
    }

    @Test
    @Throws(Exception::class)
    fun resolveHostNameToMinionHostNameTest() {
        val networkInterfaceResponse = NetworkInterfaceResponse()
        val result = ArrayList<Map<String, String>>()
        val resultMap = HashMap<String, String>()
        resultMap.put("10-0-0-1.example.com", "10.0.0.1")
        resultMap.put("10-0-0-2.example.com", "10.0.0.2")
        resultMap.put("10-0-0-3.example.com", "10.0.0.3")
        result.add(resultMap)
        networkInterfaceResponse.setResult(result)
        `when`(saltConnector!!.run(any<Target<String>>(), eq("network.interface_ip"), eq(LOCAL), eq<Class<NetworkInterfaceResponse>>(NetworkInterfaceResponse::class.java), eq("eth0"))).thenReturn(networkInterfaceResponse)

        val hostName = SaltStates.resolveHostNameToMinionHostName(saltConnector, "10-0-0-1.example.com")
        assertEquals("10.0.0.1", hostName)
    }

}