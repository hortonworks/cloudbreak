package com.sequenceiq.cloudbreak.cluster.service;

public class NotEnoughSpaceException extends RuntimeException {

    public NotEnoughSpaceException(String message) {
        super(message);
    }
}
