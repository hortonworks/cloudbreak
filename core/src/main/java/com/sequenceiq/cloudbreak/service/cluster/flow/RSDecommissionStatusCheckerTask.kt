package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RSDecommissionStatusCheckerTask : ClusterBasedStatusCheckerTask<AmbariHostsWithNames>() {

    override fun checkStatus(t: AmbariHostsWithNames): Boolean {
        MDCBuilder.buildMdcContext(t.stack)
        val ambariClient = t.ambariClient
        val rs = ambariClient.getHBaseRegionServersState(t.hostNames)
        for (entry in rs.entries) {
            if (FINAL_STATE != entry.value) {
                LOGGER.info("RegionServer: {} decommission is in progress, current state: {}", entry.key, entry.value)
                return false
            }
        }
        return true
    }

    override fun handleTimeout(t: AmbariHostsWithNames) {
        throw IllegalStateException("RegionServer decommission timed out")
    }

    override fun successMessage(t: AmbariHostsWithNames): String {
        return "Requested RegionServer decommission operations completed"
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(RSDecommissionStatusCheckerTask::class.java)
        private val FINAL_STATE = "INSTALLED"
    }

}
