package com.sequenceiq.consumption.dto;

import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ResourceType;

public class ConsumptionCreationDto {

    private final String name;

    private final String description;

    private final String accountId;

    private final String resourceCrn;

    private final String environmentCrn;

    private final ResourceType monitoredResourceType;

    private final String monitoredResourceCrn;

    private final ConsumptionType consumptionType;

    private final String storageLocation;

    @SuppressWarnings("ExecutableStatementCount")
    private ConsumptionCreationDto(Builder builder) {
        name = builder.name;
        description = builder.description;
        accountId = builder.accountId;
        resourceCrn = builder.resourceCrn;
        environmentCrn = builder.environmentCrn;
        monitoredResourceType = builder.monitoredResourceType;
        monitoredResourceCrn = builder.monitoredResourceCrn;
        consumptionType = builder.consumptionType;
        storageLocation = builder.storageLocation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public ResourceType getMonitoredResourceType() {
        return monitoredResourceType;
    }

    public String getMonitoredResourceCrn() {
        return monitoredResourceCrn;
    }

    public ConsumptionType getConsumptionType() {
        return consumptionType;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public static final class Builder {

        private String name;

        private String description;

        private String accountId;

        private String resourceCrn;

        private String environmentCrn;

        private ResourceType monitoredResourceType;

        private String monitoredResourceCrn;

        private ConsumptionType consumptionType;

        private String storageLocation;

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder withEnvironmentCrn(String environmentCrn) {
            this.environmentCrn = environmentCrn;
            return this;
        }

        public Builder withMonitoredResourceType(ResourceType monitoredResourceType) {
            this.monitoredResourceType = monitoredResourceType;
            return this;
        }

        public Builder withMonitoredResourceCrn(String monitoredResourceCrn) {
            this.monitoredResourceCrn = monitoredResourceCrn;
            return this;
        }

        public Builder withConsumptionType(ConsumptionType consumptionType) {
            this.consumptionType = consumptionType;
            return this;
        }

        public Builder withStorageLocation(String storageLocation) {
            this.storageLocation = storageLocation;
            return this;
        }

        public ConsumptionCreationDto build() {
            return new ConsumptionCreationDto(this);
        }
    }
}
