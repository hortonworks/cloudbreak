package com.sequenceiq.cloudbreak.orchestrator.marathon.poller

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import mesosphere.marathon.client.Marathon
import mesosphere.marathon.client.model.v2.App

class MarathonAppBootstrap(private val client: Marathon, private val app: App) : OrchestratorBootstrap {

    @Throws(Exception::class)
    override fun call(): Boolean? {
        val desiredTasksCount = app.instances
        val appResponse = client.getApp(this.app.id).app
        val tasksRunning = appResponse.tasksRunning

        if (tasksRunning < desiredTasksCount) {
            val msg = String.format("Marathon container '%s' instance count: '%s', desired instance count: '%s'!", app.id, tasksRunning,
                    desiredTasksCount)
            throw CloudbreakOrchestratorFailedException(msg)
        }

        return true
    }
}
