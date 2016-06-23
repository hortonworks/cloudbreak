package com.sequenceiq.cloudbreak.orchestrator

import org.junit.Assert.assertEquals

import org.junit.Test
import org.slf4j.MDC

import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel

class SimpleOrchestratorBootstrapRunnerTest {

    @Test
    @Throws(Exception::class)
    fun bootstrapSuccessWithoutException() {
        MDC.put("test", "test")
        val call = OrchestratorBootstrapRunner(MockBootstrapRunner(1, MDC.getCopyOfContextMap()),
                MockExitCriteria(),
                MockExitCriteriaModel(),
                MDC.getCopyOfContextMap()).call()
        assertEquals(true, call)
    }

    inner class MockBootstrapRunner(retryOk: Int, private val mdcMap: Map<String, String>) : OrchestratorBootstrap {

        private var count: Int = 0
        private val retryOk = 2

        init {
            this.retryOk = retryOk
        }

        @Throws(Exception::class)
        override fun call(): Boolean? {
            count++
            if (count != retryOk) {
                throw CloudbreakException("test")
            } else {
                return true
            }
        }

    }

    inner class MockExitCriteriaModel : ExitCriteriaModel()

    inner class MockExitCriteria : ExitCriteria {

        override fun isExitNeeded(exitCriteriaModel: ExitCriteriaModel): Boolean {
            return false
        }

        override fun exitMessage(): String {
            return "test"
        }
    }
}