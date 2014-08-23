package com.sequenceiq.periscope.service;

public class NoScalingGroupException extends Exception {

    private final long id;

    public NoScalingGroupException(long id, String message) {
        super(message);
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
