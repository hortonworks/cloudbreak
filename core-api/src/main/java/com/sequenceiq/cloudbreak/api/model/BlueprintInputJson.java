package com.sequenceiq.cloudbreak.api.model;

public class BlueprintInputJson implements JsonEntity {

    private String name;
    private String propertyValue;

    public BlueprintInputJson() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }
}
