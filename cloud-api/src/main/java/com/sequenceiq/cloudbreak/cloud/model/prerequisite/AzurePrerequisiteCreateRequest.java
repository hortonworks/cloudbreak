package com.sequenceiq.cloudbreak.cloud.model.prerequisite;

import java.util.Map;

public class AzurePrerequisiteCreateRequest {

    private final String resourceGroupName;

    private final String locationName;

    private final Map<String, String> tags;

    private AzurePrerequisiteCreateRequest(Builder builder) {
        resourceGroupName = builder.resourceGroupName;
        locationName = builder.locationName;
        tags = builder.tags;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public String getLocationName() {
        return locationName;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String resourceGroupName;

        private String locationName;

        private Map<String, String> tags;

        public Builder withResourceGroupName(String resourceGroupName) {
            this.resourceGroupName = resourceGroupName;
            return this;
        }

        public Builder withLocation(String locationName) {
            this.locationName = locationName;
            return this;
        }

        public Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public AzurePrerequisiteCreateRequest build() {
            return new AzurePrerequisiteCreateRequest(this);
        }
    }
}
