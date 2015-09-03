package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

import com.sequenceiq.cloudbreak.domain.ResourceType;

public class CloudResource extends DynamicModel {

    private ResourceType type;
    private String name;

    public CloudResource(ResourceType type, String name) {
        this.type = type;
        this.name = name;
    }

    public CloudResource(ResourceType type, String name, Map<String, Object> params) {
        this.type = type;
        this.name = name;
        putAll(params);
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudResource{");
        sb.append("type=").append(type);
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
