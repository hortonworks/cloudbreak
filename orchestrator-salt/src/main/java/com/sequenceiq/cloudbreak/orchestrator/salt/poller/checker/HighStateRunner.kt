package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker

import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates

class HighStateRunner(target: Set<String>, allNode: Set<Node>) : BaseSaltJobRunner(target, allNode) {

    override fun submit(saltConnector: SaltConnector): String {
        return SaltStates.highstate(saltConnector).toString()
    }

    override fun stateType(): StateType {
        return StateType.HIGH
    }

    override fun toString(): String {
        return "HighStateChecker{" + super.toString() + "}"
    }
}
