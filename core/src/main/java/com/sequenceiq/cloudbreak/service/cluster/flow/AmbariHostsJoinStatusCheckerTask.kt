package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AmbariHostsJoinStatusCheckerTask : ClusterBasedStatusCheckerTask<AmbariHostsCheckerContext>() {

    override fun checkStatus(hosts: AmbariHostsCheckerContext): Boolean {
        try {
            val ambariClient = hosts.ambariClient
            val hostNames = ambariClient.hostStatuses
            for (hostMetadata in hosts.hostsInCluster) {
                var contains = false
                for (hostName in hostNames.entries) {
                    if (hostName.key == hostMetadata.hostName && "UNKNOWN" != hostName.value) {
                        contains = true
                        break
                    }
                }
                if (!contains) {
                    LOGGER.info("The host {} currently not part of the cluster, waiting for join", hostMetadata.hostName)
                    return false
                }
            }
        } catch (e: Exception) {
            LOGGER.info("Did not join all hosts yet, polling")
            return false
        }

        return true
    }

    override fun handleTimeout(t: AmbariHostsCheckerContext) {
        LOGGER.error("Operation timed out. Failed to find all '{}' Ambari hosts. Stack: '{}'", t.hostCount, t.stack.id)
    }

    override fun successMessage(t: AmbariHostsCheckerContext): String {
        return String.format("Ambari client found all %s hosts for stack '%s'", t.hostCount, t.stack.id)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AmbariHostsJoinStatusCheckerTask::class.java)
    }

}
