package com.sequenceiq.cloudbreak.cloud.model.prerequisite;

import com.sequenceiq.common.api.tag.model.Tags;

public class AzurePrerequisiteCreateRequest {

    private final String resourceGroupName;

    private final String locationName;

    private final Tags tags;

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

    public Tags getTags() {
        return tags;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String resourceGroupName;

        private String locationName;

        private Tags tags;

        public Builder withResourceGroupName(String resourceGroupName) {
            this.resourceGroupName = resourceGroupName;
            return this;
        }

        public Builder withLocation(String locationName) {
            this.locationName = locationName;
            return this;
        }

        public Builder withTags(Tags tags) {
            this.tags = tags;
            return this;
        }

        public AzurePrerequisiteCreateRequest build() {
            return new AzurePrerequisiteCreateRequest(this);
        }
    }
}
