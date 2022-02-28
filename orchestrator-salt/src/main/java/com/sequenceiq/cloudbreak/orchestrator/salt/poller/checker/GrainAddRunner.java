package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;

public class GrainAddRunner extends ModifyGrainBase {

    public GrainAddRunner(Set<String> targetHostnames, Set<Node> allNode, String role) {
        this(targetHostnames, allNode, "roles", role);
    }

    public GrainAddRunner(Set<String> targetHostnames, Set<Node> allNode, String key, String value) {
        super(targetHostnames, allNode, key, value, true);
    }
}
