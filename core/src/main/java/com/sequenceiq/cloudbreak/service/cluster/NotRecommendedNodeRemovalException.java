package com.sequenceiq.cloudbreak.service.cluster;

public class NotRecommendedNodeRemovalException extends RuntimeException {

    public NotRecommendedNodeRemovalException(String message) {
        super(message);
    }
}
