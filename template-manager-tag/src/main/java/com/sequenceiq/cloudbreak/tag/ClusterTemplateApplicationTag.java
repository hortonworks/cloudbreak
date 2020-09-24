package com.sequenceiq.cloudbreak.tag;

public enum ClusterTemplateApplicationTag {
    SERVICE_TYPE("Cloudera-Service-Type");

    private final String key;

    ClusterTemplateApplicationTag(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
