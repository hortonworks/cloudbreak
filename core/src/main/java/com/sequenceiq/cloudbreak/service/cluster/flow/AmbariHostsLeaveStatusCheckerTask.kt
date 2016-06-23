package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AmbariHostsLeaveStatusCheckerTask : ClusterBasedStatusCheckerTask<AmbariHostsWithNames>() {

    override fun checkStatus(hosts: AmbariHostsWithNames): Boolean {
        try {
            val ambariClient = hosts.ambariClient
            val hostNames = hosts.hostNames
            val hostStatuses = ambariClient.hostStatuses
            for (hostName in hostNames) {
                val status = hostStatuses[hostName]
                if (LEFT_STATE != status || status == null) {
                    LOGGER.info("{} didn't leave the cluster yet", hostName)
                    return false
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to check the left hosts", e)
            return false
        }

        return true
    }

    override fun handleTimeout(t: AmbariHostsWithNames) {
        LOGGER.error("Operation timed out. Hosts didn't leave in time, hosts: '{}' stack: '{}'", t.hostNames, t.stack.id)
    }

    override fun successMessage(t: AmbariHostsWithNames): String {
        return String.format("Hosts left the cluster, hosts: '%s' stack '%s'", t.hostNames, t.stack.id)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AmbariHostsLeaveStatusCheckerTask::class.java)
        private val LEFT_STATE = "UNKNOWN"
    }

}
