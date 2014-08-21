package com.sequenceiq.periscope.service;

public class ClusterNotFoundException extends Exception {

    private final long id;

    public ClusterNotFoundException(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
