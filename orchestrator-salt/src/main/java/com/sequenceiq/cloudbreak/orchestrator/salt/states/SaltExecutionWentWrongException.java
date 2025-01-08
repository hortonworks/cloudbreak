package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class SaltExecutionWentWrongException extends RuntimeException {

    private final List<String> unresponsiveNodes;

    private final Multimap<String, Map<String, String>> nodesWithErrors;

    public SaltExecutionWentWrongException(String message) {
        this(message, new ArrayList<>(), ArrayListMultimap.create());
    }

    public SaltExecutionWentWrongException(String message, List<String> unresponsiveNodes, Multimap<String, Map<String, String>> nodesWithErrors) {
        super(message);
        this.unresponsiveNodes = unresponsiveNodes;
        this.nodesWithErrors = nodesWithErrors;
    }

    public List<String> getUnresponsiveNodes() {
        return unresponsiveNodes;
    }

    public Multimap<String, Map<String, String>> getNodesWithErrors() {
        return nodesWithErrors;
    }
}
