package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker

import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates

class SyncGrainsRunner(target: Set<String>, allNode: Set<Node>) : BaseSaltJobRunner(target, allNode) {

    override fun submit(saltConnector: SaltConnector): String {
        val grainsResult = SaltStates.syncGrains(saltConnector, Compound(target))
        val strings = collectMissingNodes(collectNodes(grainsResult))
        target = strings
        return strings.toString()
    }

    override fun toString(): String {
        return "SyncGrainsChecker{" + super.toString() + "}"
    }

}
