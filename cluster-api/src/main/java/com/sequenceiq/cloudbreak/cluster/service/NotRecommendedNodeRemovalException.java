package com.sequenceiq.cloudbreak.cluster.service;

public class NotRecommendedNodeRemovalException extends RuntimeException {

    public NotRecommendedNodeRemovalException(String message) {
        super(message);
    }
}
