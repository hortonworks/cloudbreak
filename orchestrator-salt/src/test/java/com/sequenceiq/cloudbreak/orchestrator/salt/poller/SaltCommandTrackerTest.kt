package com.sequenceiq.cloudbreak.orchestrator.salt.poller

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.mockito.Mockito.`when`

import java.util.HashSet

import org.junit.Test
import org.mockito.Mockito

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector

class SaltCommandTrackerTest {

    @Test
    @Throws(Exception::class)
    fun callHasTargetNodesTest() {
        val saltConnector = Mockito.mock<SaltConnector>(SaltConnector::class.java)
        val saltJobRunner = Mockito.mock<SaltJobRunner>(SaltJobRunner::class.java)
        val targets = HashSet<String>()
        targets.add("10.0.0.1")
        targets.add("10.0.0.2")
        targets.add("10.0.0.3")
        `when`(saltJobRunner.target).thenReturn(targets)
        val saltCommandTracker = SaltCommandTracker(saltConnector, saltJobRunner)
        try {
            saltCommandTracker.call()
            fail("shoud throw exception")
        } catch (e: CloudbreakOrchestratorFailedException) {
            for (target in targets) {
                assertTrue(e.message.contains(target))
            }
        }

    }

    @Test
    @Throws(Exception::class)
    fun callTest() {
        val saltConnector = Mockito.mock<SaltConnector>(SaltConnector::class.java)
        val saltJobRunner = Mockito.mock<SaltJobRunner>(SaltJobRunner::class.java)
        val targets = HashSet<String>()
        `when`(saltJobRunner.target).thenReturn(targets)
        val saltCommandTracker = SaltCommandTracker(saltConnector, saltJobRunner)
        saltCommandTracker.call()
    }

}