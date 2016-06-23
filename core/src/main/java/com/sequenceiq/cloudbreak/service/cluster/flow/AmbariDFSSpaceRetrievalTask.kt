package com.sequenceiq.cloudbreak.service.cluster.flow

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask

@Component
class AmbariDFSSpaceRetrievalTask : ClusterBasedStatusCheckerTask<AmbariClientPollerObject>() {
    var dfsSpace: Map<String, Map<Long, Long>>? = null
        private set

    override fun checkStatus(ambariClientPollerObject: AmbariClientPollerObject): Boolean {
        try {
            dfsSpace = ambariClientPollerObject.ambariClient.dfsSpace
            return true
        } catch (ex: Exception) {
            LOGGER.warn("Error during getting dfs space from ambari", ex)
            return false
        }

    }

    override fun handleTimeout(ambariClientPollerObject: AmbariClientPollerObject) {
    }

    override fun successMessage(ambariClientPollerObject: AmbariClientPollerObject): String {
        return "Dfs space successfully get from ambari."
    }

    companion object {
        val AMBARI_RETRYING_INTERVAL = 5000
        val AMBARI_RETRYING_COUNT = 3

        private val LOGGER = LoggerFactory.getLogger(AmbariDFSSpaceRetrievalTask::class.java)
    }
}
