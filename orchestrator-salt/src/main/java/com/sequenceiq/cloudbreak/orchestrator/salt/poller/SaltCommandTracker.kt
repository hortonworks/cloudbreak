package com.sequenceiq.cloudbreak.orchestrator.salt.poller

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector

class SaltCommandTracker(private val saltConnector: SaltConnector, private val saltJobRunner: SaltJobRunner) : OrchestratorBootstrap {

    @Throws(Exception::class)
    override fun call(): Boolean? {
        saltJobRunner.submit(saltConnector)
        if (!saltJobRunner.target.isEmpty()) {
            throw CloudbreakOrchestratorFailedException("There are missing nodes from job result: " + saltJobRunner.target)
        }
        return true
    }

    override fun toString(): String {
        return "SaltCommandTracker{"
        +"saltJobRunner=" + saltJobRunner
        +'}'
    }
}
