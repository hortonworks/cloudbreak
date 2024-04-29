package com.sequenceiq.cloudbreak.common.orchestration;

import java.util.Set;

public interface OrchestratorAware {

    Set<Node> getAllFunctioningNodes();

    Set<Node> getAllNotDeletedNodes();
}
