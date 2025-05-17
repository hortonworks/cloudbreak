package com.sequenceiq.cloudbreak.cloud.model;

public record InstanceCheckMetadata(
        String instanceId,
        String instanceType,
        InstanceStatus status
) {
    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .withInstanceId(instanceId)
                .withInstanceType(instanceType)
                .withStatus(status);
    }

    public static final class Builder {

        private String instanceId;

        private String instanceType;

        private InstanceStatus status;

        public Builder withInstanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder withInstanceType(String instanceType) {
            this.instanceType = instanceType;
            return this;
        }

        public Builder withStatus(InstanceStatus status) {
            this.status = status;
            return this;
        }

        public InstanceCheckMetadata build() {
            return new InstanceCheckMetadata(instanceId, instanceType, status);
        }
    }
}
