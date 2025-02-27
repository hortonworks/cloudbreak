package com.sequenceiq.common.model;

public enum DefaultApplicationTag {

    owner("owner"),
    CREATION_TIMESTAMP("creation-timestamp"),
    ENVIRONMENT_CRN("Cloudera-Environment-Resource-Name"),
    CREATOR_CRN("Cloudera-Creator-Resource-Name"),
    RESOURCE_CRN("Cloudera-Resource-Name"),
    RESOURCE_ID("Cloudera-Resource-ID");

    private final String key;

    DefaultApplicationTag(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
