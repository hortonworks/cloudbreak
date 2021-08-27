package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Cloudbreak handles the entities on the Cloud provider side as Generic resources, and this class represent a generic resource.
 */
public class CloudResource extends DynamicModel {

    public static final String IMAGE = "IMAGE";

    public static final String ATTRIBUTES = "attributes";

    public static final String ATTRIBUTE_TYPE = "attributeType";

    public static final String PRIVATE_ID = "privateId";

    public static final String INSTANCE_TYPE = "instanceType";

    private final ResourceType type;

    private CommonStatus status;

    private final String name;

    private String reference;

    private final String group;

    private final String availabilityZone;

    private final boolean persistent;

    private final boolean stackAware;

    private String instanceId;

    private CloudResource(ResourceType type, CommonStatus status, String name, String reference, String group, boolean persistent, Map<String, Object> params,
            String instanceId, boolean stackAware, String availabilityZone) {
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

        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder stackAware(boolean stackAware) {
            this.stackAware = stackAware;
            return this;
        }

        public Builder availabilityZone(String availabilityZone) {
            this.availabilityZone = availabilityZone;
            return this;
        }

        public CloudResource build() {
            Preconditions.checkNotNull(type);
            Preconditions.checkNotNull(status);
            Preconditions.checkNotNull(name);
            Preconditions.checkNotNull(parameters);
            if (Objects.isNull(stackAware)) {
                return new CloudResource(type, status, name, reference, group, persistent, parameters, instanceId, true, availabilityZone);
            } else {
                return new CloudResource(type, status, name, reference, group, persistent, parameters, instanceId, stackAware, availabilityZone);
            }
        }
    }
}
