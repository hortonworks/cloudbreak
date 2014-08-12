package com.sequenceiq.periscope.rest;

public class ClusterNotFoundException extends RuntimeException {

    private final String id;

    public ClusterNotFoundException(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
