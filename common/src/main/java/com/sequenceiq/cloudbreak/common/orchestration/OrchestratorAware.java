package com.sequenceiq.cloudbreak.common.orchestration;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;

public interface OrchestratorAware {

    default Set<OrchestrationNode> getAllNodesForOrchestration() {
        throw new NotImplementedException("Needs to be implemented for the default operation or override the `getAllNodes()`");
    }

    default Set<Node> getAllNodes() {
        return getAllNodesForOrchestration().stream().map(OrchestrationNode::getNode).collect(Collectors.toSet());
    }
}
