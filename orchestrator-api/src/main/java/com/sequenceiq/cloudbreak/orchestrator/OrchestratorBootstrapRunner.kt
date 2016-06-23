package com.sequenceiq.cloudbreak.orchestrator

import java.util.concurrent.Callable

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel

class OrchestratorBootstrapRunner @JvmOverloads constructor(private val orchestratorBootstrap: OrchestratorBootstrap, private val exitCriteria: ExitCriteria?,
                                                            private val exitCriteriaModel: ExitCriteriaModel?, private val mdcMap: Map<String, String>?,
                                                            private val maxRetryCount: Int = OrchestratorBootstrapRunner.MAX_RETRY_COUNT, private val sleepTime: Int = OrchestratorBootstrapRunner.SLEEP_TIME) : Callable<Boolean> {

    @Throws(Exception::class)
    override fun call(): Boolean? {
        if (mdcMap != null) {
            MDC.setContextMap(mdcMap)
        }
        var success = false
        var retryCount = 1
        var actualException: Exception? = null
        val type = orchestratorBootstrap.javaClass.getSimpleName().replace("Bootstrap", "")
        while (!success && retryCount <= maxRetryCount) {
            if (isExitNeeded) {
                LOGGER.error(exitCriteria!!.exitMessage())
                throw CloudbreakOrchestratorCancelledException(exitCriteria.exitMessage())
            }
            val startTime = System.currentTimeMillis()
            try {
                LOGGER.info("Calling orchestrator bootstrap: {}, additional info: {}", type, orchestratorBootstrap)
                orchestratorBootstrap.call()
                val elapsedTime = System.currentTimeMillis() - startTime
                success = true
                LOGGER.info("Orchestrator component {} successfully started! Elapsed time: {} ms, additional info: {}", type, elapsedTime,
                        orchestratorBootstrap)
            } catch (ex: Exception) {
                val elapsedTime = System.currentTimeMillis() - startTime
                actualException = ex
                LOGGER.warn("Orchestrator component {} failed to start, retrying [{}/{}] Elapsed time: {} ms; Reason: {}, additional info: {}", type,
                        retryCount, maxRetryCount, elapsedTime, actualException.message, orchestratorBootstrap)
                retryCount++
                if (retryCount <= maxRetryCount) {
                    Thread.sleep(sleepTime.toLong())
                }
            }

        }

        if (!success) {
            LOGGER.error(String.format("Orchestrator component failed to start in %s attempts: %s", maxRetryCount, actualException))
            throw actualException
        }
        return java.lang.Boolean.TRUE
    }

    private val isExitNeeded: Boolean
        get() {
            var exitNeeded = false
            if (exitCriteriaModel != null && exitCriteria != null) {
                LOGGER.debug("exitCriteriaModel: {}, exitCriteria: {}", exitCriteriaModel, exitNeeded)
                exitNeeded = exitCriteria.isExitNeeded(exitCriteriaModel)
            }
            LOGGER.debug("isExitNeeded: {}", exitNeeded)
            return exitNeeded
        }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(OrchestratorBootstrapRunner::class.java)
        private val MAX_RETRY_COUNT = 30
        private val SLEEP_TIME = 5000
    }
}
