package com.sequenceiq.environment.parameters.dto;

import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupCreation;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupUsagePattern;

public class AzureResourceGroupDto {

    private final String name;

    private final ResourceGroupUsagePattern resourceGroupUsagePattern;

    private final ResourceGroupCreation resourceGroupCreation;

    public AzureResourceGroupDto(Builder builder) {
        name = builder.name;
        resourceGroupUsagePattern = builder.resourceGroupUsagePattern;
        resourceGroupCreation = builder.resourceGroupCreation;
    }

    public String getName() {
        return name;
    }

    public ResourceGroupUsagePattern getResourceGroupUsagePattern() {
        return resourceGroupUsagePattern;
    }

    public ResourceGroupCreation getResourceGroupCreation() {
        return resourceGroupCreation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;

        private ResourceGroupUsagePattern resourceGroupUsagePattern;

        private ResourceGroupCreation resourceGroupCreation;

        public Builder withName(String resourceGroupName) {
            this.name = resourceGroupName;
            return this;
        }

        public Builder withResourceGroupUsagePattern(ResourceGroupUsagePattern resourceGroupUsagePattern) {
            this.resourceGroupUsagePattern = resourceGroupUsagePattern;
            return this;
        }

        public Builder withResourceGroupCreation(ResourceGroupCreation resourceGroupCreation) {
            this.resourceGroupCreation = resourceGroupCreation;
            return this;
        }

        public AzureResourceGroupDto build() {
            return new AzureResourceGroupDto(this);
        }
    }
}
