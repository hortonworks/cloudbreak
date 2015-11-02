package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.cloudbreak.common.type.CommonStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

import reactor.core.support.Assert;

/**
 * Cloudbreak handles the entities on the Cloud provider side as Generic resources, and this class represent a generic resource.
 */
public class CloudResource extends DynamicModel {
    private ResourceType type;
    private CommonStatus status;
    private String name;
    private String reference;
    private boolean persistent;

    private CloudResource(ResourceType type, CommonStatus status, String name, String reference, boolean persistent, Map<String, Object> params) {
        super(params);
        this.type = type;
        this.status = status;
        this.name = name;
        this.reference = reference;
        this.persistent = persistent;
    }

    public ResourceType getType() {
        return type;
    }

    public CommonStatus getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public String getReference() {
        return reference;
    }

    public boolean isPersistent() {
        return persistent;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudResource{");
        sb.append("type=").append(type);
        sb.append(", status=").append(status);
        sb.append(", name='").append(name).append('\'');
        sb.append(", reference='").append(reference).append('\'');
        sb.append(", persistent='").append(persistent).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static class Builder {
        private ResourceType type;
        private CommonStatus status = CommonStatus.CREATED;
        private String name;
        private String reference;
        private boolean persistent = true;
        private Map<String, Object> parameters = new HashMap<>();

        public Builder cloudResource(CloudResource cloudResource) {
            this.type = cloudResource.getType();
            this.status = cloudResource.getStatus();
            this.name = cloudResource.getName();
            this.reference = cloudResource.getReference();
            this.persistent = cloudResource.isPersistent();
            return this;
        }

        public Builder type(ResourceType type) {
            this.type = type;
            return this;
        }

        public Builder status(CommonStatus status) {
            this.status = status;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder persistent(boolean persistent) {
            this.persistent = persistent;
            return this;
        }

        public Builder params(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public CloudResource build() {
            Assert.notNull(type);
            Assert.notNull(status);
            Assert.notNull(name);
            Assert.notNull(parameters);
            return new CloudResource(type, status, name, reference, persistent, parameters);
        }
    }
}
