package com.sequenceiq.cloudbreak.core.bootstrap.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.runners.MockitoJUnitRunner

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack

@RunWith(MockitoJUnitRunner::class)
class StackDeletionBasedExitCriteriaTest {

    @InjectMocks
    private val underTest: StackDeletionBasedExitCriteria? = null

    @Test
    fun exitNeededScenariosTest() {
        val stack = TestUtil.stack()
        val cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L)
        stack.cluster = cluster
        val exitCriteriaModel = StackDeletionBasedExitCriteriaModel(1L)

        InMemoryStateStore.putStack(1L, PollGroup.POLLABLE)
        assertFalse(underTest!!.isExitNeeded(exitCriteriaModel))

        InMemoryStateStore.putStack(1L, PollGroup.CANCELLED)
        assertTrue(underTest.isExitNeeded(exitCriteriaModel))

    }

}