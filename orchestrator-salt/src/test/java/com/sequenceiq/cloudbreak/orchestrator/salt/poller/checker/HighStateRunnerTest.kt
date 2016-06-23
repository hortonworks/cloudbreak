package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker

import org.junit.Assert.assertEquals
import org.mockito.Matchers.any
import org.mockito.Matchers.eq

import java.util.HashSet

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates

@RunWith(PowerMockRunner::class)
@PrepareForTest(SaltStates::class)
class HighStateRunnerTest {

    private var targets: MutableSet<String>? = null
    private var allNode: MutableSet<Node>? = null

    @Test
    @Throws(Exception::class)
    fun submit() {
        targets = HashSet<String>()
        targets!!.add("10.0.0.1")
        targets!!.add("10.0.0.2")
        targets!!.add("10.0.0.3")
        allNode = HashSet<Node>()
        allNode!!.add(Node("10.0.0.1", "5.5.5.1", "10-0-0-1.example.com"))
        allNode!!.add(Node("10.0.0.2", "5.5.5.2", "10-0-0-2.example.com"))
        allNode!!.add(Node("10.0.0.3", "5.5.5.3", "10-0-0-3.example.com"))

        val highStateRunner = HighStateRunner(targets, allNode)

        val saltConnector = Mockito.mock<SaltConnector>(SaltConnector::class.java)

        PowerMockito.mockStatic(SaltStates::class.java)
        val jobId = "1"
        PowerMockito.`when`(SaltStates.highstate(any<SaltConnector>())).thenReturn(jobId)

        val jid = highStateRunner.submit(saltConnector)
        assertEquals(jobId, jid)
        PowerMockito.verifyStatic()
        SaltStates.highstate(eq(saltConnector))
    }

    @Test
    @Throws(Exception::class)
    fun stateType() {
        assertEquals(StateType.HIGH, HighStateRunner(targets, allNode).stateType())
    }

}