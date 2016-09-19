package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class BlueprintInput implements ProvisionEntity {

    private String name;
    @Column(columnDefinition = "TEXT")
    private String propertyValue;

    public BlueprintInput() {

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
