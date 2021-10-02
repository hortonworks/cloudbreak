package com.sequenceiq.environment.environment.dto;

import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;

public class UpdateAzureResourceEncryptionDto {

    private AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto;

    public AzureResourceEncryptionParametersDto getAzureResourceEncryptionParametersDto() {
        return azureResourceEncryptionParametersDto;
    }

    public void setAzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto) {
        this.azureResourceEncryptionParametersDto = azureResourceEncryptionParametersDto;
    }

    public static UpdateAzureResourceEncryptionDto.Builder builder() {
        return new UpdateAzureResourceEncryptionDto.Builder();
    }

    @Override
    public String toString() {
        return "UpdateAzureResourceEncryptionDto{" +
                "AzureResourceEncryptionParametersDto='" + azureResourceEncryptionParametersDto + '\'' +
                '}';
    }

    public static final class Builder {

        private AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto;

        public Builder withAzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto) {
            this.azureResourceEncryptionParametersDto = azureResourceEncryptionParametersDto;
            return this;
        }

        public UpdateAzureResourceEncryptionDto build() {
            UpdateAzureResourceEncryptionDto updateAzureResourceEncryptionDto = new UpdateAzureResourceEncryptionDto();
            updateAzureResourceEncryptionDto.setAzureResourceEncryptionParametersDto(azureResourceEncryptionParametersDto);
            return updateAzureResourceEncryptionDto;
        }
    }
}
