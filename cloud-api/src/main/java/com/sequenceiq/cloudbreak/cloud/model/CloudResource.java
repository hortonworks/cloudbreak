package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.domain.ResourceType;

public class CloudResource {

    private ResourceType type;
    private String reference;

    public CloudResource(ResourceType type, String reference) {
        this.type = type;
        this.reference = reference;
    }

    public ResourceType getType() {
        return type;
    }

    public String getReference() {
        return reference;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudResource{");
        sb.append("type=").append(type);
        sb.append(", reference='").append(reference).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
