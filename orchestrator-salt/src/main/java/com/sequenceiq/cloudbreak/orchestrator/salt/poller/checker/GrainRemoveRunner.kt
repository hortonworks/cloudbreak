package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker

import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound

class GrainRemoveRunner(target: Set<String>, allNode: Set<Node>, key: String, value: String, type: Compound.CompoundType) : ModifyGrainBase(target, allNode, key, value, type, false)
