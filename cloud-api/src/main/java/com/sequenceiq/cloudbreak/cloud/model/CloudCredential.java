package com.sequenceiq.cloudbreak.cloud.model;

public class CloudCredential extends DynamicModel {

    private String name;

    public CloudCredential(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
