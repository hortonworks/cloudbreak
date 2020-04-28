package com.sequenceiq.environment.parameters.dto;

public class AzureParametersDto {

    private AzureResourceGroupDto azureResourceGroupDto;

    public AzureParametersDto(Builder builder) {
        azureResourceGroupDto = builder.azureResourceGroupDto;
    }

    public AzureResourceGroupDto getAzureResourceGroupDto() {
        return azureResourceGroupDto;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private AzureResourceGroupDto azureResourceGroupDto;

        public Builder withResourceGroup(AzureResourceGroupDto azureResourceGroupDto) {
            this.azureResourceGroupDto = azureResourceGroupDto;
            return this;
        }

        public AzureParametersDto build() {
            return new AzureParametersDto(this);
        }
    }
}
