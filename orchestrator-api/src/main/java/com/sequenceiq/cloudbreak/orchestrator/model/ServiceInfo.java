package com.sequenceiq.cloudbreak.orchestrator.model;

public class ServiceInfo {

    private String name;
    private String host;

    public ServiceInfo(String name, String host) {
        this.name = name;
        this.host = host;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

}
