package com.sequenceiq.cloudbreak.api.model;

import io.swagger.annotations.ApiModel;

@ApiModel("RdsConfigProperty")
public class RdsConfigPropertyJson implements JsonEntity {

    private String name;

    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
