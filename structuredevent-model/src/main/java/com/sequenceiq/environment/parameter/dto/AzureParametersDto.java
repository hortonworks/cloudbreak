package com.sequenceiq.environment.parameter.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = AzureParametersDto.Builder.class)
public class AzureParametersDto {

    private final AzureResourceGroupDto azureResourceGroupDto;

    private final AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto;

    private AzureParametersDto(Builder builder) {
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

    @JsonPOJOBuilder
    public static final class Builder {

        private AzureResourceGroupDto azureResourceGroupDto;

        private AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto;

        public Builder withAzureResourceGroupDto(AzureResourceGroupDto azureResourceGroupDto) {
            this.azureResourceGroupDto = azureResourceGroupDto;
            return this;
        }

        public Builder withAzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto) {
            this.azureResourceEncryptionParametersDto = azureResourceEncryptionParametersDto;
            return this;
        }

        public AzureParametersDto build() {
            return new AzureParametersDto(this);
        }
    }
}
