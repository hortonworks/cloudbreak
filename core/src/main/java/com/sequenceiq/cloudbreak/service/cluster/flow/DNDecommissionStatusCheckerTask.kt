package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DNDecommissionStatusCheckerTask : ClusterBasedStatusCheckerTask<AmbariOperations>() {

    override fun checkStatus(t: AmbariOperations): Boolean {
        val ambariClient = t.ambariClient
        val dataNodes = ambariClient.decommissioningDataNodes
        val finished = dataNodes.isEmpty()
        if (!finished) {
            LOGGER.info("DataNode decommission is in progress: {}", dataNodes)
        }
        return finished
    }

    override fun handleTimeout(t: AmbariOperations) {
        throw IllegalStateException("DataNode decommission timed out")

    }

    override fun successMessage(t: AmbariOperations): String {
        return "Requested DataNode decommission operations completed"
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(DNDecommissionStatusCheckerTask::class.java)
    }

}
