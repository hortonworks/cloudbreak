package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker

import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound

class GrainAddRunner(target: Set<String>, allNode: Set<Node>, key: String, value: String, type: Compound.CompoundType) : ModifyGrainBase(target, allNode, key, value, type, true) {

    constructor(target: Set<String>, allNode: Set<Node>, role: String) : this(target, allNode, "roles", role, Compound.CompoundType.IP) {
    }
}
