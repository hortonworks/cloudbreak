package com.sequenceiq.cloudbreak.cloud.model.prerequisite;

public class AzurePrerequisiteDeleteRequest {
    private final String resourceGroupName;

    public AzurePrerequisiteDeleteRequest(Builder builder) {
        this.resourceGroupName = builder.resourceGroupName;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String resourceGroupName;

        public Builder withResourceGroupName(String resourceGroupName) {
            this.resourceGroupName = resourceGroupName;
            return this;
        }

        public AzurePrerequisiteDeleteRequest build() {
            return new AzurePrerequisiteDeleteRequest(this);
        }

    }
}
