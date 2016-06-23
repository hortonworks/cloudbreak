package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker

import org.hamcrest.core.IsCollectionContaining.hasItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.mockito.Matchers.any
import org.mockito.Matchers.anyString

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates

@RunWith(PowerMockRunner::class)
@PrepareForTest(SaltStates::class)
class GrainAddRunnerTest {

    private var targets: MutableSet<String>? = null
    private var allNode: MutableSet<Node>? = null

    @Test
    @Throws(Exception::class)
    fun submitTest() {
        targets = HashSet<String>()
        targets!!.add("10.0.0.1")
        targets!!.add("10.0.0.2")
        targets!!.add("10.0.0.3")
        allNode = HashSet<Node>()
        allNode!!.add(Node("10.0.0.1", "5.5.5.1", "10-0-0-1.example.com"))
        allNode!!.add(Node("10.0.0.2", "5.5.5.2", "10-0-0-2.example.com"))
        allNode!!.add(Node("10.0.0.3", "5.5.5.3", "10-0-0-3.example.com"))

        PowerMockito.mockStatic(SaltStates::class.java)
        val applyResponse = ApplyResponse()
        val result = ArrayList<Map<String, Any>>()
        val nodes = HashMap<String, Any>()
        nodes.put("10-0-0-1.example.com", "something")
        nodes.put("10-0-0-2.example.com", "something")
        result.add(nodes)
        applyResponse.result = result
        PowerMockito.`when`(SaltStates.addGrain(any<SaltConnector>(), any<Target<String>>(), anyString(), any<String>())).thenReturn(applyResponse)

        val addRoleChecker = GrainAddRunner(targets, allNode, "ambari_server")

        val saltConnector = Mockito.mock<SaltConnector>(SaltConnector::class.java)
        val missingIps = addRoleChecker.submit(saltConnector)
        assertThat(addRoleChecker.target, hasItems("10.0.0.3"))
        assertEquals("[10.0.0.3]", missingIps)
    }

}