package com.sequenceiq.cloudbreak.service.cluster.flow

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask
import com.sequenceiq.cloudbreak.service.cluster.AmbariHostsUnavailableException

@Component
class AmbariHostsStatusCheckerTask : ClusterBasedStatusCheckerTask<AmbariHostsCheckerContext>() {

    override fun checkStatus(t: AmbariHostsCheckerContext): Boolean {
        val healthyHostNames = t.ambariClient.getHostNamesByState("HEALTHY")
        val unHealthyHostNames = t.ambariClient.getHostNamesByState("UNHEALTHY")
        val alertHostNames = t.ambariClient.getHostNamesByState("ALERT")
        val unknownHostNames = t.ambariClient.getHostNamesByState("UNKNOWN")
        val totalNodes = healthyHostNames.size + unHealthyHostNames.size + alertHostNames.size + unknownHostNames.size
        LOGGER.info("Ambari client found {} hosts ({} needed). [Stack: '{}']", totalNodes, t.hostsInCluster.size, t.stack.id)
        if (totalNodes >= t.hostsInCluster.size) {
            return true
        }
        return false
    }

    override fun handleTimeout(t: AmbariHostsCheckerContext) {
        throw AmbariHostsUnavailableException(String.format("Operation timed out. Failed to find all '%s' Ambari hosts. Stack: '%s'",
                t.hostsInCluster.size, t.stack.id))
    }

    override fun successMessage(t: AmbariHostsCheckerContext): String {
        return String.format("Ambari client found all %s hosts for stack '%s'", t.hostsInCluster.size, t.stack.id)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AmbariHostsStatusCheckerTask::class.java)
    }

}
