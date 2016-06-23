package com.sequenceiq.cloudbreak.service.cluster.flow

import com.google.common.base.Optional
import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import java.math.BigDecimal

@Component
class AmbariOperationsStartCheckerTask : ClusterBasedStatusCheckerTask<AmbariOperations>() {

    override fun checkStatus(t: AmbariOperations): Boolean {
        val installRequests = t.requests
        var allStarted = true
        for (request in installRequests.entries) {
            val ambariClient = t.ambariClient
            val installProgress = Optional.fromNullable(ambariClient.getRequestProgress(request.value)).or(PENDING)
            LOGGER.info("Ambari operation start: '{}', Progress: {}", request.key, installProgress)
            allStarted = allStarted && COMPLETED.compareTo(installProgress) != 0 && PENDING.compareTo(installProgress) != 0
            if (FAILED.compareTo(installProgress) == 0) {
                var failed = true
                for (i in 0..MAX_RETRY - 1) {
                    if (ambariClient.getRequestProgress(request.value).compareTo(FAILED) != 0) {
                        failed = false
                        break
                    }
                }
                if (failed) {
                    throw AmbariOperationFailedException(String.format("Ambari operation start failed: [component:'%s', requestID: '%s']", request.key,
                            request.value))
                }
            }
        }
        return allStarted
    }

    override fun handleTimeout(t: AmbariOperations) {
        throw IllegalStateException(String.format("Ambari operations start timed out: %s", t.requests))
    }

    override fun successMessage(t: AmbariOperations): String {
        return String.format("Requested Ambari operations started: %s", t.requests.toString())
    }

    override fun handleException(e: Exception) {
        LOGGER.error("Ambari operation start failed.", e)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AmbariOperationsStartCheckerTask::class.java)

        private val COMPLETED = BigDecimal(100.0)
        private val FAILED = BigDecimal(-1.0)
        private val PENDING = BigDecimal(0)
        private val MAX_RETRY = 3
    }

}
