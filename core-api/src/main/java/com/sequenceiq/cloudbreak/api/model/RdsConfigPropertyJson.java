package com.sequenceiq.cloudbreak.api.model;


public class RdsConfigPropertyJson implements JsonEntity {

    private String name;
    private String value;

    public RdsConfigPropertyJson() {

    }

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
