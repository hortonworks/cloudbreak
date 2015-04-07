package com.sequenceiq.cloudbreak.service.cluster.flow;

public enum DockerContainer {

    AMBARI_SERVER("ambari-server"),
    AMBARI_AGENT("ambari-agent"),
    AMBARI_DB("ambari_db");

    private final String name;

    private DockerContainer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
