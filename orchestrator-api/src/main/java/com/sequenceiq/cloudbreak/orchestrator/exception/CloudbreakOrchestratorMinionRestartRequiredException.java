package com.sequenceiq.cloudbreak.orchestrator.exception;

import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class CloudbreakOrchestratorMinionRestartRequiredException extends CloudbreakOrchestratorFailedException {

    public CloudbreakOrchestratorMinionRestartRequiredException(String message, Set<String> minionIdToRestart) {
        super(message, toNodesWithErrors(minionIdToRestart));
    }

    public CloudbreakOrchestratorMinionRestartRequiredException(String message, Throwable cause, Set<String> minionIdToRestart) {
        super(message, cause, toNodesWithErrors(minionIdToRestart));
    }

    public CloudbreakOrchestratorMinionRestartRequiredException(String message, CloudbreakOrchestratorException cause, Set<String> minionIdToRestart) {
        super(message, cause, mergeNodesWithErrors(cause, minionIdToRestart));
    }

    private static Multimap<String, String> mergeNodesWithErrors(CloudbreakOrchestratorException cause, Set<String> minionIdToRestart) {
        Multimap<String, String> nodesWithErrors = toNodesWithErrors(minionIdToRestart);
        if (cause != null) {
            nodesWithErrors.putAll(cause.getNodesWithErrors());
        }
        return nodesWithErrors;
    }

    private static Multimap<String, String> toNodesWithErrors(Set<String> minionIdToRestart) {
        Multimap<String, String> nodesWithErrors = ArrayListMultimap.create();
        minionIdToRestart.forEach(minionId -> nodesWithErrors.put(minionId, "Minion needs to be restarted"));
        return nodesWithErrors;
    }
}
