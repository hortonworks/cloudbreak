package com.sequenceiq.environment.environment.dto;

import com.sequenceiq.environment.parameter.dto.AwsDiskEncryptionParametersDto;

public class UpdateAwsDiskEncryptionParametersDto {

    private AwsDiskEncryptionParametersDto awsDiskEncryptionParametersDto;

    public AwsDiskEncryptionParametersDto getAwsDiskEncryptionParametersDto() {
        return awsDiskEncryptionParametersDto;
    }

    public void setAwsDiskEncryptionParametersDto(AwsDiskEncryptionParametersDto awsDiskEncryptionParametersDto) {
        this.awsDiskEncryptionParametersDto = awsDiskEncryptionParametersDto;
    }

    public static UpdateAwsDiskEncryptionParametersDto.Builder builder() {
        return new UpdateAwsDiskEncryptionParametersDto.Builder();
    }

    @Override
    public String toString() {
        return "UpdateAwsDiskEncryptionParametersDto{" +
                "AwsDiskEncryptionParametersDto='" + awsDiskEncryptionParametersDto + '\'' +
                '}';
    }

    public static final class Builder {

        private AwsDiskEncryptionParametersDto awsDiskEncryptionParametersDto;

        public Builder withAwsDiskEncryptionParametersDto(AwsDiskEncryptionParametersDto awsDiskEncryptionParametersDto) {
            this.awsDiskEncryptionParametersDto = awsDiskEncryptionParametersDto;
            return this;
        }

        public UpdateAwsDiskEncryptionParametersDto build() {
            UpdateAwsDiskEncryptionParametersDto updateAwsDiskEncryptionParametersDto = new UpdateAwsDiskEncryptionParametersDto();
            updateAwsDiskEncryptionParametersDto.setAwsDiskEncryptionParametersDto(awsDiskEncryptionParametersDto);
            return updateAwsDiskEncryptionParametersDto;
        }
    }
}
