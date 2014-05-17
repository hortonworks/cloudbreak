package com.sequenceiq.provisioning.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AwsStackJson implements JsonEntity {

    @JsonProperty("name")
    private String name;

    public AwsStackJson() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
