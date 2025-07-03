package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.common.api.type.CommonResourceType;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Cloudbreak handles the entities on the Cloud provider side as Generic resources, and this class represent a generic resource.
 */
@JsonDeserialize(builder = CloudResource.Builder.class)
public class CloudResource extends DynamicModel {

    public static final String IMAGE = "IMAGE";

    public static final String ATTRIBUTES = "attributes";

    public static final String ATTRIBUTE_TYPE = "attributeType";

    public static final String PRIVATE_ID = "privateId";

    public static final String INSTANCE_TYPE = "instanceType";

    public static final String ARCHITECTURE = "architecture";

    private final ResourceType type;

    private CommonStatus status;

    private final String name;

    private final String reference;

    private final String group;

    private final String availabilityZone;

    private final boolean persistent;

    private final boolean stackAware;

    private final Long privateId;

    private String instanceId;

    private CloudResource(ResourceType type, CommonStatus status, String name, String reference, String group, boolean persistent, Map<String, Object> params,
            String instanceId, boolean stackAware, String availabilityZone, Long privateId) {
        super(params);
        this.type = type;
        this.status = status;
        this.name = name;
        this.reference = reference;
        this.group = group;
        this.persistent = persistent;
        this.instanceId = instanceId;
        this.stackAware = stackAware;
        this.availabilityZone = availabilityZone;
        this.privateId = privateId;
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

    public boolean isStackAware() {
        return stackAware;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setStatus(CommonStatus status) {
        this.status = status;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public Long getPrivateId() {
        return privateId;
    }

    public String getDetailedInfo() {
        if (instanceId != null && !name.equals(instanceId)) {
            return getType() + " - " + getName() + " (" + instanceId + ")";
        } else {
            return getType() + " - " + getName();
        }
    }

    public CommonResourceType getCommonResourceType() {
        return type.getCommonResourceType();
    }

    public <T> void setTypedAttributes(T attributes) {
        putParameter(ATTRIBUTES, attributes);
        putParameter(ATTRIBUTE_TYPE, attributes.getClass().getCanonicalName());
    }

    public <T> T getTypedAttributes(Class<T> type, Supplier<T> defaultSupplier) {
        T attributes = getParameter(ATTRIBUTES, type);
        return attributes == null ? defaultSupplier.get() : attributes;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CloudResource.class.getSimpleName() + "[", "]")
                .add("type=" + type)
                .add("status=" + status)
                .add("name='" + name + "'")
                .add("reference='" + reference + "'")
                .add("group='" + group + "'")
                .add("persistent=" + persistent)
                .add("stackAware=" + stackAware)
                .add("instanceId='" + instanceId + "'")
                .add("availabilityZone='" + availabilityZone + "'")
                .add("parameters='" + getParameters() + "'")
                .toString();
    }

    @Override
    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CloudResource that = (CloudResource) o;
        return persistent == that.persistent && stackAware == that.stackAware && type == that.type && status == that.status && Objects.equals(name, that.name)
                && Objects.equals(reference, that.reference) && Objects.equals(group, that.group) && Objects.equals(availabilityZone, that.availabilityZone)
                && Objects.equals(instanceId, that.instanceId) && Objects.equals(getParameters(), that.getParameters())
                && Objects.equals(privateId, that.privateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, status, name, reference, group, availabilityZone, persistent, stackAware, instanceId, privateId, getParameters());
    }

    @JsonPOJOBuilder
    public static class Builder {
        private ResourceType type;

        private CommonStatus status = CommonStatus.CREATED;

        private String name;

        private String reference;

        private boolean persistent = true;

        private String group;

        private String instanceId;

        private Map<String, Object> parameters = new HashMap<>();

        private Boolean stackAware;

        private String availabilityZone;

        private Long privateId;

        private Builder() {
        }

        public Builder cloudResource(CloudResource cloudResource) {
            type = cloudResource.getType();
            status = cloudResource.getStatus();
            name = cloudResource.getName();
            reference = cloudResource.getReference();
            persistent = cloudResource.isPersistent();
            group = cloudResource.getGroup();
            instanceId = cloudResource.getInstanceId();
            stackAware = cloudResource.isStackAware();
            availabilityZone = cloudResource.getAvailabilityZone();
            privateId = cloudResource.getPrivateId();
            return this;
        }

        public Builder withType(ResourceType type) {
            this.type = type;
            return this;
        }

        public Builder withStatus(CommonStatus status) {
            this.status = status;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withReference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder withPersistent(boolean persistent) {
            this.persistent = persistent;
            return this;
        }

        @JsonAlias({"params"})
        public Builder withParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder withGroup(String group) {
            this.group = group;
            return this;
        }

        public Builder withInstanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder withStackAware(boolean stackAware) {
            this.stackAware = stackAware;
            return this;
        }

        public Builder withAvailabilityZone(String availabilityZone) {
            this.availabilityZone = availabilityZone;
            return this;
        }

        public Builder withPrivateId(Long privateId) {
            this.privateId = privateId;
            return this;
        }

        public CloudResource build() {
            Preconditions.checkNotNull(type);
            Preconditions.checkNotNull(status);
            Preconditions.checkNotNull(name);
            Preconditions.checkNotNull(parameters);
            if (Objects.isNull(stackAware)) {
                return new CloudResource(type, status, name, reference, group, persistent, parameters, instanceId, true, availabilityZone, privateId);
            } else {
                return new CloudResource(type, status, name, reference, group, persistent, parameters, instanceId, stackAware, availabilityZone, privateId);
            }
        }
    }
}