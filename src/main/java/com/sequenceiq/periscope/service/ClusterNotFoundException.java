package com.sequenceiq.periscope.service;

public class ClusterNotFoundException extends Exception {

    private final String id;

    public ClusterNotFoundException(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
