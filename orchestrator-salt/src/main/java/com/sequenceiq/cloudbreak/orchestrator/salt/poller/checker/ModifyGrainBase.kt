package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker

import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates

abstract class ModifyGrainBase(target: Set<String>, allNode: Set<Node>, private val key: String, private val value: String, private val compoundType: Compound.CompoundType, private val addGrain: Boolean) : BaseSaltJobRunner(target, allNode) {

    override fun submit(saltConnector: SaltConnector): String {
        val response: ApplyResponse
        if (addGrain) {
            response = SaltStates.addGrain(saltConnector, Compound(target, compoundType), key, value)
        } else {
            response = SaltStates.removeGrain(saltConnector, Compound(target, compoundType), key, value)
        }
        val missingIps = collectMissingNodes(collectNodes(response))
        target = missingIps
        return missingIps.toString()
    }

    override fun toString(): String {
        val sb = StringBuilder("ModifyGrainBase{")
        sb.append("key='").append(key).append('\'')
        sb.append(", value='").append(value).append('\'')
        sb.append(", compoundType=").append(compoundType)
        sb.append(", addGrain=").append(addGrain)
        sb.append('}')
        return sb.toString()
    }
}
