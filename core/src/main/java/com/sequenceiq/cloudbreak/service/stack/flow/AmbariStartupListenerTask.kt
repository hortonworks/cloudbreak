package com.sequenceiq.cloudbreak.service.stack.flow

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException

@Component
class AmbariStartupListenerTask : ClusterBasedStatusCheckerTask<AmbariStartupPollerObject>() {

    override fun checkStatus(aSPO: AmbariStartupPollerObject): Boolean {
        var ambariRunning = false
        LOGGER.info("Polling Ambari server's status [Ambari server address: '{}'].", aSPO.ambariAddress)
        try {
            val ambariHealth = aSPO.ambariClient.healthCheck()
            LOGGER.info("Ambari health check returned: {} [Ambari server address: '{}']", ambariHealth, aSPO.ambariAddress)
            if ("RUNNING" == ambariHealth) {
                ambariRunning = true
            }
        } catch (e: Exception) {
            LOGGER.info("Ambari health check failed: {}", e.message)
        }

        return ambariRunning
    }

    override fun handleTimeout(ambariStartupPollerObject: AmbariStartupPollerObject) {
        throw AmbariOperationFailedException("Operation timed out. Failed to check ambari startup.")
    }

    override fun successMessage(aSPO: AmbariStartupPollerObject): String {
        return "Ambari startup finished with success result."
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AmbariStartupListenerTask::class.java)
    }
}
