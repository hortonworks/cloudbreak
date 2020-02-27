package com.sequenceiq.cloudbreak.api.endpoint.v4.common;

public enum ParcelType {

    CLOUDERA_RUNTIME("Cloudera Runtime"),

    SCHEMA_REGISTRY("Schema Registry"),

    STREAMS_MESSAGING_MANAGER("Streams Messaging Manager"),

    CFM("Cloudera Flow Management");

    private String name;

    ParcelType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
