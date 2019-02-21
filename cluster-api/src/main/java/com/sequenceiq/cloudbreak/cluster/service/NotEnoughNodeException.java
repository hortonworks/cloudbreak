package com.sequenceiq.cloudbreak.cluster.service;

public class NotEnoughNodeException extends RuntimeException {

    public NotEnoughNodeException(String message) {
        super(message);
    }
}
