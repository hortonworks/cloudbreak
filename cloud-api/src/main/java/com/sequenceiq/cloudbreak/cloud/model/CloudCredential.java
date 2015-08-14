package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

public class CloudCredential extends DynamicModel {

    private String name;

    public CloudCredential(String name) {
        this.name = name;
    }

    public CloudCredential(String name, Map<String, Object> parameters) {
        this.name = name;
        super.putAll(parameters);
    }

    public String getName() {
        return name;
    }

}
