package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AmbariHealthCheckerTask : ClusterBasedStatusCheckerTask<AmbariClientPollerObject>() {

    override fun checkStatus(ambariClientPollerObject: AmbariClientPollerObject): Boolean {
        try {
            val ambariHealth = ambariClientPollerObject.ambariClient.healthCheck()
            if ("RUNNING" == ambariHealth) {
                return true
            }
            return false
        } catch (e: Exception) {
            LOGGER.info("Ambari is not running yet: {}", e.message)
            return false
        }

    }

    override fun handleTimeout(t: AmbariClientPollerObject) {
        throw CloudbreakServiceException(String.format("Operation timed out. Ambari server could not start %s", t.ambariClient.ambari.uri))
    }

    override fun successMessage(t: AmbariClientPollerObject): String {
        return String.format("Ambari server successfully started '%s'", t.ambariClient.ambari.uri)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AmbariHealthCheckerTask::class.java)
    }

}
