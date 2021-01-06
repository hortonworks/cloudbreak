package com.sequenceiq.cloudbreak.cluster.model;

public class ServiceLocation {
    private final String service;

    private final String volumePath;

    public ServiceLocation(String service, String volumePath) {
        this.service = service;
        this.volumePath = volumePath;
    }

    public String getService() {
        return service;
    }

    public String getVolumePath() {
        return volumePath;
    }
}
