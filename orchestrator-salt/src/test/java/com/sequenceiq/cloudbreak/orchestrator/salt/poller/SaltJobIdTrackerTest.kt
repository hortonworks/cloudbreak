package com.sequenceiq.cloudbreak.orchestrator.salt.poller

import org.hamcrest.Matchers.both
import org.hamcrest.core.StringContains.containsString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.mockito.Matchers.any
import org.mockito.Matchers.eq
import org.mockito.Mockito.doCallRealMethod
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.util.HashSet

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates

@RunWith(PowerMockRunner::class)
@PrepareForTest(SaltStates::class)
class SaltJobIdTrackerTest {


    @Captor
    private val targetCaptor: ArgumentCaptor<Target<String>>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Throws(Exception::class)
    fun callWithNotStarted() {
        val jobId = "1"
        val saltConnector = Mockito.mock<SaltConnector>(SaltConnector::class.java)
        val saltJobRunner = Mockito.mock<SaltJobRunner>(SaltJobRunner::class.java)
        PowerMockito.mockStatic(SaltStates::class.java)
        PowerMockito.`when`(SaltStates.jobIsRunning(any<SaltConnector>(), any<String>(), any<Target<String>>())).thenReturn(true)

        val targets = HashSet<String>()
        targets.add("10.0.0.1")
        targets.add("10.0.0.2")
        targets.add("10.0.0.3")
        `when`(saltJobRunner.target).thenReturn(targets)
        `when`(saltJobRunner.jid).thenReturn(JobId.jobId(jobId))
        `when`(saltJobRunner.jobState).thenReturn(JobState.NOT_STARTED, JobState.IN_PROGRESS)
        `when`(saltJobRunner.submit(any<SaltConnector>(SaltConnector::class.java))).thenReturn(jobId)
        val saltJobIdTracker = SaltJobIdTracker(saltConnector, saltJobRunner)
        try {
            saltJobIdTracker.call()
            fail("should throw exception")
        } catch (e: CloudbreakOrchestratorFailedException) {
            assertThat<String>(e.message, both(containsString("jobId='$jobId'")).and(containsString("is running")))
        }

        PowerMockito.verifyStatic()
        SaltStates.jobIsRunning(any<SaltConnector>(), eq(jobId), targetCaptor!!.capture())
        checkTargets(targets, targetCaptor.allValues)
        verify(saltJobRunner, times(2)).jobState
    }

    @Test
    @Throws(Exception::class)
    fun callWithFailed() {
        val jobId = "1"
        val saltConnector = Mockito.mock<SaltConnector>(SaltConnector::class.java)
        PowerMockito.mockStatic(SaltStates::class.java)
        PowerMockito.`when`(SaltStates.jobIsRunning(any<SaltConnector>(), any<String>(), any<Target<String>>())).thenReturn(true)

        val saltJobRunner = Mockito.mock<BaseSaltJobRunner>(BaseSaltJobRunner::class.java)
        `when`(saltJobRunner.jid).thenReturn(JobId.jobId(jobId))
        `when`(saltJobRunner.jobState).thenCallRealMethod()
        doCallRealMethod().`when`<SaltJobRunner>(saltJobRunner).jobState = any<JobState>()

        `when`(saltJobRunner.submit(any<SaltConnector>(SaltConnector::class.java))).thenReturn(jobId)
        saltJobRunner.jobState = JobState.FAILED

        val saltJobIdTracker = SaltJobIdTracker(saltConnector, saltJobRunner)
        try {
            saltJobIdTracker.call()
            fail("should throw exception")
        } catch (e: CloudbreakOrchestratorFailedException) {
            assertThat<String>(e.message, both(containsString("jobId='$jobId'")).and(containsString("is running")))
        }

        PowerMockito.verifyStatic()
        SaltStates.jobIsRunning(any<SaltConnector>(), eq(jobId), any<Target<String>>())
    }

    private fun checkTargets(targets: Set<String>, allValues: List<Target<String>>) {
        for (allValue in allValues) {
            for (target in targets) {
                assertThat(allValue.target, containsString(target))
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun callWithInProgressAndJobIsRunning() {
        val jobId = "1"
        val saltConnector = Mockito.mock<SaltConnector>(SaltConnector::class.java)

        val saltJobRunner = Mockito.mock<BaseSaltJobRunner>(BaseSaltJobRunner::class.java)
        `when`(saltJobRunner.jid).thenReturn(JobId.jobId(jobId))
        `when`(saltJobRunner.jobState).thenCallRealMethod()
        doCallRealMethod().`when`<SaltJobRunner>(saltJobRunner).jobState = any<JobState>()

        `when`(saltJobRunner.submit(any<SaltConnector>(SaltConnector::class.java))).thenReturn(jobId)
        saltJobRunner.jobState = JobState.IN_PROGRESS

        val targets = HashSet<String>()
        targets.add("10.0.0.1")
        targets.add("10.0.0.2")
        targets.add("10.0.0.3")
        `when`(saltJobRunner.target).thenReturn(targets)

        PowerMockito.mockStatic(SaltStates::class.java)
        PowerMockito.`when`(SaltStates.jobIsRunning(any<SaltConnector>(), any<String>(), any<Target<String>>())).thenReturn(true)

        val saltJobIdTracker = SaltJobIdTracker(saltConnector, saltJobRunner)
        try {
            saltJobIdTracker.call()
        } catch (e: CloudbreakOrchestratorFailedException) {
            assertThat<String>(e.message, both(containsString("jobId='$jobId'")).and(containsString("is running")))
        }

        PowerMockito.verifyStatic()
        SaltStates.jobIsRunning(any<SaltConnector>(), eq(jobId), targetCaptor!!.capture())
        checkTargets(targets, targetCaptor.allValues)
    }

    @Test
    @Throws(Exception::class)
    fun callWithInProgressAndJobIsFinished() {
        val jobId = "1"
        val saltConnector = Mockito.mock<SaltConnector>(SaltConnector::class.java)

        val saltJobRunner = Mockito.mock<BaseSaltJobRunner>(BaseSaltJobRunner::class.java)
        `when`(saltJobRunner.jid).thenReturn(JobId.jobId(jobId))
        `when`(saltJobRunner.jobState).thenCallRealMethod()
        doCallRealMethod().`when`<SaltJobRunner>(saltJobRunner).jobState = any<JobState>()

        `when`(saltJobRunner.submit(any<SaltConnector>(SaltConnector::class.java))).thenReturn(jobId)
        saltJobRunner.jobState = JobState.IN_PROGRESS

        val targets = HashSet<String>()
        targets.add("10.0.0.1")
        targets.add("10.0.0.2")
        targets.add("10.0.0.3")
        `when`(saltJobRunner.target).thenReturn(targets)

        PowerMockito.mockStatic(SaltStates::class.java)
        PowerMockito.`when`(SaltStates.jobIsRunning(any<SaltConnector>(), any<String>(), any<Target<String>>())).thenReturn(false)

        val missingNodesWithReason = ArrayListMultimap.create<String, String>()
        PowerMockito.`when`(SaltStates.jidInfo(any<SaltConnector>(), any<String>(), any<Target<String>>(), any<StateType>())).thenReturn(missingNodesWithReason)

        val saltJobIdTracker = SaltJobIdTracker(saltConnector, saltJobRunner)
        assertTrue(saltJobIdTracker.call()!!)

        assertEquals(JobState.FINISHED, saltJobRunner.jobState)

        PowerMockito.verifyStatic()
        SaltStates.jobIsRunning(any<SaltConnector>(), eq(jobId), targetCaptor!!.capture())
        checkTargets(targets, targetCaptor.allValues)
    }

    @Test
    @Throws(Exception::class)
    fun callWithInProgressAndMissingNodes() {
        val jobId = "1"
        val saltConnector = Mockito.mock<SaltConnector>(SaltConnector::class.java)

        val saltJobRunner = Mockito.mock<BaseSaltJobRunner>(BaseSaltJobRunner::class.java)
        `when`(saltJobRunner.jid).thenReturn(JobId.jobId(jobId))
        `when`(saltJobRunner.jobState).thenCallRealMethod()
        doCallRealMethod().`when`<SaltJobRunner>(saltJobRunner).jobState = any<JobState>()

        `when`(saltJobRunner.submit(any<SaltConnector>(SaltConnector::class.java))).thenReturn(jobId)
        saltJobRunner.jobState = JobState.IN_PROGRESS

        val targets = HashSet<String>()
        targets.add("10.0.0.1")
        targets.add("10.0.0.2")
        targets.add("10.0.0.3")
        `when`(saltJobRunner.target).thenReturn(targets)

        PowerMockito.mockStatic(SaltStates::class.java)
        PowerMockito.`when`(SaltStates.jobIsRunning(any<SaltConnector>(), any<String>(), any<Target<String>>())).thenReturn(false)

        val missingNodesWithReason = ArrayListMultimap.create<String, String>()
        val missingMachine = "10.0.0.1"
        val errorMessage = "error happened"
        missingNodesWithReason.put(missingMachine, errorMessage)
        PowerMockito.`when`(SaltStates.jidInfo(any<SaltConnector>(), any<String>(), any<Target<String>>(), any<StateType>())).thenReturn(missingNodesWithReason)

        val saltJobIdTracker = SaltJobIdTracker(saltConnector, saltJobRunner)
        try {
            saltJobIdTracker.call()
            fail("should throw exception")
        } catch (e: CloudbreakOrchestratorFailedException) {
            assertThat<String>(e.message, both(containsString(missingMachine)).and(containsString(errorMessage)))
        }

        PowerMockito.verifyStatic()
        SaltStates.jobIsRunning(any<SaltConnector>(), eq(jobId), targetCaptor!!.capture())
        checkTargets(targets, targetCaptor.allValues)
    }

}