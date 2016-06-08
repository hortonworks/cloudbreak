package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;

public class GrainRemoveRunner extends ModifyGrainBase {

    public GrainRemoveRunner(Set<String> target, Set<Node> allNode, String key, String value, Compound.CompoundType type) {
        super(target, allNode, key, value, type, false);
    }
}
