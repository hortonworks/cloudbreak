package com.sequenceiq.environment.parameters.dto;

public class AzureResourceGroupDto {

    private final String name;

    private final Boolean single;

    private final Boolean existing;

    public AzureResourceGroupDto(Builder builder) {
        name = builder.name;
        single = builder.single;
        existing = builder.existing;
    }

    public String getName() {
        return name;
    }

    public Boolean isSingle() {
        return single;
    }

    public Boolean isExisting() {
        return existing;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;

        private Boolean single;

        private Boolean existing;

        public Builder withName(String resourceGroupName) {
            this.name = resourceGroupName;
            return this;
        }

        public Builder withSingle(Boolean single) {
            this.single = single;
            return this;
        }

        public Builder withExisting(Boolean existing) {
            this.existing = existing;
            return this;
        }

        public AzureResourceGroupDto build() {
            return new AzureResourceGroupDto(this);
        }
    }
}
