package com.sequenceiq.cloudbreak.cloud.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureRoleDefinition {

    private String id;

    private String name;

    private String type;

    private AzureRoleDefinitionProperties properties;

    public AzureRoleDefinition() {
    }

    public AzureRoleDefinition(String id, String name, String type, AzureRoleDefinitionProperties properties) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public AzureRoleDefinitionProperties getProperties() {
        return properties;
    }

    public void setProperties(AzureRoleDefinitionProperties properties) {
        this.properties = properties;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "AzureRoleDefinition{"
                + "id='" + id + '\''
                + ", name='" + name + '\''
                + ", type='" + type + '\''
                + ", properties=" + properties
                + '}';
    }
    //END GENERATED CODE
}
