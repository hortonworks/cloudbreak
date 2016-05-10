package com.sequenceiq.cloudbreak.orchestrator.container;

public enum HostServiceType {

    AMBARI_SERVER("ambari-server"),
    AMBARI_AGENT("ambari-agent");

    private final String name;

    HostServiceType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }


}
