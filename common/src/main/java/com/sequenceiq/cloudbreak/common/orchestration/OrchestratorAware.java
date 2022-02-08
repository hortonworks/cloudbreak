package com.sequenceiq.cloudbreak.common.orchestration;

import java.util.Set;
import java.util.stream.Collectors;

public interface OrchestratorAware {

    Set<? extends OrchestrationNode> getAllNodesForOrchestration();

    default Set<Node> getAllNodes() {
        return getAllNodesForOrchestration().stream().map(OrchestrationNode::getNode).collect(Collectors.toSet());
    }
}
