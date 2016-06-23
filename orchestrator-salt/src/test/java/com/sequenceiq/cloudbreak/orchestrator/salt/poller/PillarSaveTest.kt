package com.sequenceiq.cloudbreak.orchestrator.salt.poller

import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.util.HashMap
import java.util.HashSet

import org.junit.Assert
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.http.HttpStatus

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar

class PillarSaveTest {

    @Test
    fun testPillarProperties() {
        val saltConnector = Mockito.mock<SaltConnector>(SaltConnector::class.java)
        val pillarJson = HashMap<String, Any>()
        val pillarProperties = SaltPillarProperties("/nodes/hosts.sls", pillarJson)
        PillarSave(saltConnector, pillarProperties)
    }

    @Test
    @Throws(Exception::class)
    fun testDiscovery() {
        val saltConnector = Mockito.mock<SaltConnector>(SaltConnector::class.java)
        val response = GenericResponse()
        response.statusCode = HttpStatus.OK.value()
        `when`(saltConnector.pillar(Mockito.any<Pillar>(Pillar::class.java))).thenReturn(response)

        val nodes = HashSet<Node>()
        nodes.add(Node("10.0.0.1", "1.1.1.1", "10-0-0-1.example.com"))
        nodes.add(Node("10.0.0.2", "1.1.1.2", "10-0-0-2.example.com"))
        nodes.add(Node("10.0.0.3", "1.1.1.3", "10-0-0-3.example.com"))
        val pillarSave = PillarSave(saltConnector, nodes)
        pillarSave.call()
        val pillarCaptor = ArgumentCaptor.forClass<Pillar>(Pillar::class.java)
        verify(saltConnector).pillar(pillarCaptor.capture())
        val pillar = pillarCaptor.value
        val pillarJson = pillar.json as Map<String, Map<String, Map<String, Any>>>
        val hostMap = pillarJson.entries.iterator().next().value
        for (node in nodes) {
            Assert.assertEquals(node.hostname, hostMap.get(node.privateIp).get("fqdn"))
            Assert.assertEquals(node.hostname!!.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0], hostMap.get(node.privateIp).get("hostname"))
            Assert.assertEquals(java.lang.Boolean.TRUE, hostMap.get(node.privateIp).get("public_address"))
        }
    }

    @Test
    @Throws(Exception::class)
    fun testCall() {
        val saltConnector = Mockito.mock<SaltConnector>(SaltConnector::class.java)
        val response = GenericResponse()
        response.statusCode = HttpStatus.OK.value()
        `when`(saltConnector.pillar(Mockito.any<Pillar>(Pillar::class.java))).thenReturn(response)
        val pillarSave = PillarSave(saltConnector, "10.0.0.1")
        val callResult = pillarSave.call()
        Assert.assertTrue(callResult!!)
    }

    @Test
    fun testCallWithNotFound() {
        val saltConnector = Mockito.mock<SaltConnector>(SaltConnector::class.java)
        val response = GenericResponse()
        response.statusCode = HttpStatus.NOT_FOUND.value()
        `when`(saltConnector.pillar(Mockito.any<Pillar>(Pillar::class.java))).thenReturn(response)
        val pillarSave = PillarSave(saltConnector, "10.0.0.1")
        try {
            pillarSave.call()
            Assert.fail("Exception should happen")
        } catch (e: Exception) {
            Assert.assertEquals(CloudbreakOrchestratorFailedException::class.java, e.javaClass)
        }

    }
}