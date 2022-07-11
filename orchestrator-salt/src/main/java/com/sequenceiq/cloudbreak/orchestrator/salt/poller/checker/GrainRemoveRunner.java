package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

public class GrainRemoveRunner extends ModifyGrainBase {

    public GrainRemoveRunner(SaltStateService saltStateService, Set<String> target, Set<Node> allNode, String role) {
        this(saltStateService, target, allNode, "roles", role);
    }

    public GrainRemoveRunner(SaltStateService saltStateService, Set<String> target, Set<Node> allNode, String key, String value) {
        super(saltStateService, target, allNode, key, value, false);
    }
}
