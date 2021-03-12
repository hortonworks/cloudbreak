package com.sequenceiq.cloudbreak.util;

import java.util.Set;

public class NodesUnreachableException extends Exception {

    private final Set<String> unreachableNodes;

    public NodesUnreachableException(String message, Set<String> unreachableNodes) {
        super(message);
        this.unreachableNodes = unreachableNodes;
    }

    public Set<String> getUnreachableNodes() {
        return unreachableNodes;
    }

}
