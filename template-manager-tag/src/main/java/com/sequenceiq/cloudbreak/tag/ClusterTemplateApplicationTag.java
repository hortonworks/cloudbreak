package com.sequenceiq.cloudbreak.tag;

public enum ClusterTemplateApplicationTag {
    SERVICE_TYPE("Cloudera-Service-Type"), SERVICE_FEATURE("Cloudera-Service-Feature");

    private final String key;

    ClusterTemplateApplicationTag(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
