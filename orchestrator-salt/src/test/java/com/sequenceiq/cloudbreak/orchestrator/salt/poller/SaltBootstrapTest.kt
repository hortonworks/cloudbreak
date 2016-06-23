package com.sequenceiq.cloudbreak.orchestrator.salt.poller

import org.hamcrest.core.IsNot.not
import org.hamcrest.core.StringContains.containsString
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet

import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.NetworkInterfaceResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction

class SaltBootstrapTest {

    private var saltConnector: SaltConnector? = null
    private var gatewayConfig: GatewayConfig? = null
    private var networkMap: MutableMap<String, String>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        saltConnector = mock<SaltConnector>(SaltConnector::class.java)
        gatewayConfig = GatewayConfig("1.1.1.1", "10.0.0.1", "10-0-0-1.example.com",
                9443, "certDir", "serverCert", "clientCert", "clientKey")

        val response = GenericResponse()
        response.statusCode = HttpStatus.OK.value()
        val genericResponses = GenericResponses()
        genericResponses.responses = listOf<GenericResponse>(response)

        `when`(saltConnector!!.action(Mockito.any<SaltAction>(SaltAction::class.java))).thenReturn(genericResponses)

        val networkInterfaceResponse = NetworkInterfaceResponse()
        val networkResultList = ArrayList<Map<String, String>>()
        networkMap = HashMap<String, String>()
        networkMap!!.put("host-10-0-0-1.example.com", "10.0.0.1")
        networkMap!!.put("host-10-0-0-2.example.com", "10.0.0.2")
        networkMap!!.put("host-10-0-0-3.example.com", "10.0.0.3")
        networkResultList.add(networkMap)
        networkInterfaceResponse.setResult(networkResultList)
        `when`(saltConnector!!.run(Mockito.any<Target<String>>(), Mockito.eq("network.interface_ip"), Mockito.any<SaltClientType>(), Mockito.any<Class<Any>>(), *Mockito.any<String>())).thenReturn(networkInterfaceResponse)
    }

    @Test
    fun callTest() {
        val targets = HashSet<Node>()
        targets.add(Node("10.0.0.1", null, null))
        targets.add(Node("10.0.0.2", null, null))
        targets.add(Node("10.0.0.3", null, null))

        val saltBootstrap = SaltBootstrap(saltConnector, gatewayConfig, targets)
        try {
            saltBootstrap.call()
        } catch (e: Exception) {
            throw RuntimeException(e)
            //            fail(e.toString());
        }

    }

    @Test
    fun callFailTest() {
        networkMap!!.clear()
        networkMap!!.put("host-10-0-0-1.example.com", "10.0.0.1")
        networkMap!!.put("host-10-0-0-2.example.com", "10.0.0.2")

        val targets = HashSet<Node>()
        targets.add(Node("10.0.0.1", null, null))
        targets.add(Node("10.0.0.2", null, null))
        val missingNodeIp = "10.0.0.3"
        targets.add(Node(missingNodeIp, null, null))

        val saltBootstrap = SaltBootstrap(saltConnector, gatewayConfig, targets)
        try {
            saltBootstrap.call()
            fail("should throw exception")
        } catch (e: Exception) {
            assertTrue(CloudbreakOrchestratorFailedException::class.java!!.getSimpleName() == e.javaClass.getSimpleName())
            assertThat<String>(e.message, containsString("10.0.0.3"))
            assertThat<String>(e.message, not(containsString("10.0.0.2")))
            assertThat<String>(e.message, not(containsString("10.0.0.1")))
        }

    }

}