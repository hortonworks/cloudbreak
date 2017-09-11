package com.sequenceiq.cloudbreak.api.model;

import io.swagger.annotations.ApiModel;

@ApiModel("BlueprintParameter")
public class BlueprintParameterJson implements JsonEntity {

    private String name;

    private String description;

    private String referenceConfiguration;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceConfiguration() {
        return referenceConfiguration;
    }

    public void setReferenceConfiguration(String referenceConfiguration) {
        this.referenceConfiguration = referenceConfiguration;
    }
}
