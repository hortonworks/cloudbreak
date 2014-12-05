package com.sequenceiq.periscope.service;

public class ScalingPolicyNotFoundException extends RuntimeException {

    private final long id;

    public ScalingPolicyNotFoundException(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
