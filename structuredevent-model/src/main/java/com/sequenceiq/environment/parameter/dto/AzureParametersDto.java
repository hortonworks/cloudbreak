package com.sequenceiq.environment.parameter.dto;

public class AzureParametersDto {

    private AzureResourceGroupDto azureResourceGroupDto;

    private AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto;

    public AzureParametersDto(Builder builder) {
        azureResourceGroupDto = builder.azureResourceGroupDto;
        azureResourceEncryptionParametersDto = builder.azureResourceEncryptionParametersDto;
    }

    public AzureResourceGroupDto getAzureResourceGroupDto() {
        return azureResourceGroupDto;
    }

    public AzureResourceEncryptionParametersDto getAzureResourceEncryptionParametersDto() {
        return azureResourceEncryptionParametersDto;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AzureParametersDto{"
                + "azureResourceGroupDto=" + azureResourceGroupDto
                + ", azureResourceEncryptionParametersDto=" + azureResourceEncryptionParametersDto
                + '}';
    }

    public static final class Builder {

        private AzureResourceGroupDto azureResourceGroupDto;

        private AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto;

        public Builder withResourceGroup(AzureResourceGroupDto azureResourceGroupDto) {
            this.azureResourceGroupDto = azureResourceGroupDto;
            return this;
        }

        public Builder withEncryptionParameters(AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto) {
            this.azureResourceEncryptionParametersDto = azureResourceEncryptionParametersDto;
            return this;
        }

        public AzureParametersDto build() {
            return new AzureParametersDto(this);
        }
    }
}
