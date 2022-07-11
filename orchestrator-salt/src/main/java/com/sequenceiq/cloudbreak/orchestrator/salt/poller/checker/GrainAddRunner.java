package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

public class GrainAddRunner extends ModifyGrainBase {

    public GrainAddRunner(SaltStateService saltStateService, Set<String> targetHostnames, Set<Node> allNode, String role) {
        this(saltStateService, targetHostnames, allNode, "roles", role);
    }

    public GrainAddRunner(SaltStateService saltStateService, Set<String> targetHostnames, Set<Node> allNode, String key, String value) {
        super(saltStateService, targetHostnames, allNode, key, value, true);
    }
}
