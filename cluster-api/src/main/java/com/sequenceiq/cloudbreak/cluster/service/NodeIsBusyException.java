package com.sequenceiq.cloudbreak.cluster.service;

public class NodeIsBusyException extends RuntimeException {

    public NodeIsBusyException(String message) {
        super(message);
    }

}
