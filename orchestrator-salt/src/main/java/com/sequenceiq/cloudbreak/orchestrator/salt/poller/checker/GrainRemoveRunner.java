package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.model.Node;

public class GrainRemoveRunner extends ModifyGrainBase {

    public GrainRemoveRunner(Set<String> target, Set<Node> allNode, String key, String value) {
        super(target, allNode, key, value, false);
    }
}
