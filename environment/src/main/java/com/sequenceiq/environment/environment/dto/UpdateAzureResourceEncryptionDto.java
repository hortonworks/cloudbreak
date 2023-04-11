package com.sequenceiq.environment.environment.dto;

import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;

public class UpdateAzureResourceEncryptionDto {

    private AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto;

    public UpdateAzureResourceEncryptionDto(Builder builder) {
        this.azureResourceEncryptionParametersDto = builder.azureResourceEncryptionParametersDto;
    }

    public AzureResourceEncryptionParametersDto getAzureResourceEncryptionParametersDto() {
        return azureResourceEncryptionParametersDto;
    }

    public void setAzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto) {
        this.azureResourceEncryptionParametersDto = azureResourceEncryptionParametersDto;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "UpdateAzureResourceEncryptionDto{" +
                "AzureResourceEncryptionParametersDto='" + azureResourceEncryptionParametersDto + '\'' +
                '}';
    }

    public static final class Builder {

        private AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto;

        private Builder() {
        }

        public Builder withAzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto) {
            this.azureResourceEncryptionParametersDto = azureResourceEncryptionParametersDto;
            return this;
        }

        public UpdateAzureResourceEncryptionDto build() {
            return new UpdateAzureResourceEncryptionDto(this);
        }
    }
}
