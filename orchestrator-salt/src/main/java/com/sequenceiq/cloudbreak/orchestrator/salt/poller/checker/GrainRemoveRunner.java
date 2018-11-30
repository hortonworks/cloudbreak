package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound.CompoundType;

public class GrainRemoveRunner extends ModifyGrainBase {

    public GrainRemoveRunner(Set<String> target, Set<Node> allNode, String role) {
        this(target, allNode, "roles", role, CompoundType.IP);
    }

    public GrainRemoveRunner(Set<String> target, Set<Node> allNode, String key, String value, CompoundType type) {
        super(target, allNode, key, value, type, false);
    }
}
