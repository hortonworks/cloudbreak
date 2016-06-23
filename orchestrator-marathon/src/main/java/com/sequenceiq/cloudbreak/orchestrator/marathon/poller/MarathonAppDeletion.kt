package com.sequenceiq.cloudbreak.orchestrator.marathon.poller

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import mesosphere.marathon.client.Marathon
import mesosphere.marathon.client.utils.MarathonException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MarathonAppDeletion(private val client: Marathon, private val appId: String) : OrchestratorBootstrap {

    @Throws(Exception::class)
    override fun call(): Boolean? {
        try {
            client.getApp(appId).app
            throw CloudbreakOrchestratorFailedException(String.format("Marathon app '%s' hasn't been deleted yet.", appId))
        } catch (me: MarathonException) {
            if (STATUS_NOT_FOUND == me.status) {
                LOGGER.info("Marathon app has been deleted successfully with name: '{}'", appId)
            } else {
                throw CloudbreakOrchestratorFailedException(me)
            }
        }

        return null
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MarathonAppDeletion::class.java)
        private val STATUS_NOT_FOUND = 404
    }
}
