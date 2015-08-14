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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudResource{");
        sb.append("type=").append(type);
        sb.append(", name='").append(name).append('\'');
        sb.append(", reference='").append(reference).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
