package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.domain.ResourceType;

public class CloudResource {

    private ResourceType type;

    private String name;

    private String reference;


    public CloudResource(ResourceType type, String name, String reference) {
        this.type = type;
        this.name = name;
        this.reference = reference;
    }

    public ResourceType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getReference() {
        return reference;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "CloudResource{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", reference='" + reference + '\'' +
                '}';
    }
    //END GENERATED CODE
}
