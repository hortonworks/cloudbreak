package com.sequenceiq.cloudbreak.service.cluster;

public class NotEnoughNodeException extends RuntimeException {

    public NotEnoughNodeException(String message) {
        super(message);
    }
}
