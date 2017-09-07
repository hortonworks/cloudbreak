package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.cloudbreak.common.type.CommonStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

/**
 * Cloudbreak handles the entities on the Cloud provider side as Generic resources, and this class represent a generic resource.
 */
public class CloudResource extends DynamicModel {

    private final ResourceType type;

    private final CommonStatus status;

    private final String name;

    private final String reference;

    private final String group;

    private final boolean persistent;

    private CloudResource(ResourceType type, CommonStatus status, String name, String reference, String group, boolean persistent, Map<String, Object> params) {
        super(params);
        this.type = type;
        this.status = status;
        this.name = name;
        this.reference = reference;
        this.group = group;
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

    public String getGroup() {
        return group;
    }

    public boolean isPersistent() {
        return persistent;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CloudResource{");
        sb.append("type=").append(type);
        sb.append(", status=").append(status);
        sb.append(", name='").append(name).append('\'');
        sb.append(", reference='").append(reference).append('\'');
        sb.append(", group='").append(group).append('\'');
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

        private String group;

        private Map<String, Object> parameters = new HashMap<>();

        public Builder cloudResource(CloudResource cloudResource) {
            type = cloudResource.getType();
            status = cloudResource.getStatus();
            name = cloudResource.getName();
            reference = cloudResource.getReference();
            persistent = cloudResource.isPersistent();
            group = cloudResource.getGroup();
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

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public CloudResource build() {
            Preconditions.checkNotNull(type);
            Preconditions.checkNotNull(status);
            Preconditions.checkNotNull(name);
            Preconditions.checkNotNull(parameters);
            return new CloudResource(type, status, name, reference, group, persistent, parameters);
        }
    }
}
