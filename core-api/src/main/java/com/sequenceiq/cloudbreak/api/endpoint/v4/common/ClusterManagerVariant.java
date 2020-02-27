package com.sequenceiq.cloudbreak.api.endpoint.v4.common;

public enum ClusterManagerVariant {

    AMBARI("Ambari"),

    CLOUDERA_MANAGER("Cloudera Manager");

    private String name;

    ClusterManagerVariant(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
