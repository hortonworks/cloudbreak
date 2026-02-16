package com.sequenceiq.notification.scheduled.register.dto.clusterhealth;

import static com.sequenceiq.notification.util.NotificationUtil.toCamelCase;

import java.util.Set;

public class InstanceStatusDto {

    private static final Set<String> FAILURE_TERMS = Set.of("failed", "error", "terminated", "timed_out");

    private final String name;

    private final String status;

    private final String groupName;

    private final String instanceType;

    private final String statusColor;

    private final String url;

    private InstanceStatusDto(Builder builder) {
        this.name = builder.name;
        this.status = builder.status;
        this.groupName = builder.groupName;
        this.instanceType = builder.instanceType;
        this.statusColor = builder.statusColor;
        this.url = builder.url;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public String getUrl() {
        return url;
    }

    public String getStatusColor() {
        return statusColor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name;

        private String status;

        private String groupName;

        private String instanceType;

        private String statusColor;

        private String url;

        public Builder name(String instanceName) {
            this.name = instanceName;
            return this;
        }

        public Builder status(String instanceStatus) {
            this.status = toCamelCase(instanceStatus);
            this.statusColor = FAILURE_TERMS.stream().anyMatch(status::contains) ? "#B00020" : "#FF550D";
            return this;
        }

        public Builder groupName(String instanceGroupName) {
            this.groupName = instanceGroupName;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder instanceType(String providerInstanceType) {
            this.instanceType = providerInstanceType;
            return this;
        }

        public InstanceStatusDto build() {
            return new InstanceStatusDto(this);
        }
    }
}
