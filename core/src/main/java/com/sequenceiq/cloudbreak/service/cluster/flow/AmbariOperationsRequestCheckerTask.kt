package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AmbariOperationsRequestCheckerTask : ClusterBasedStatusCheckerTask<AmbariOperations>() {

    override fun checkStatus(operations: AmbariOperations): Boolean {
        val requestContext = operations.requestContext
        val requestStatus = operations.requestStatus
        val id = operations.ambariClient.getRequestIdWithContext(requestContext, requestStatus)
        return id != -1
    }

    override fun handleTimeout(t: AmbariOperations) {
        throw IllegalStateException(String.format("Ambari request operation timed out: %s", t.requestContext))
    }

    override fun successMessage(t: AmbariOperations): String {
        return String.format("Ambari request operation started: %s", t.requestContext)
    }

    override fun handleException(e: Exception) {
        LOGGER.error("Ambari request operation failed", e)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AmbariOperationsRequestCheckerTask::class.java)
    }

}
