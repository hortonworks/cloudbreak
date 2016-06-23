package com.sequenceiq.cloudbreak.orchestrator.marathon.poller

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import mesosphere.marathon.client.Marathon
import mesosphere.marathon.client.model.v2.Task
import mesosphere.marathon.client.utils.MarathonException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MarathonTaskDeletion(private val client: Marathon, private val appId: String, private val taskIds: Set<String>) : OrchestratorBootstrap {

    @Throws(Exception::class)
    override fun call(): Boolean? {
        try {
            val tasks = client.getApp(appId).app.tasks
            for (task in tasks) {
                if (taskIds.contains(task.id)) {
                    throw CloudbreakOrchestratorFailedException(String.format("Task '%s' hasn't been deleted yet.", task.id))
                }
            }
        } catch (me: MarathonException) {
            if (STATUS_NOT_FOUND == me.status) {
                LOGGER.info("Marathon app '{}' has already been deleted.", appId)
            } else {
                throw CloudbreakOrchestratorFailedException(me)
            }
        }

        return true
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MarathonAppDeletion::class.java)
        private val STATUS_NOT_FOUND = 404
    }
}

