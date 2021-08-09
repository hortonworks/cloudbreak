package com.sequenceiq.environment.parameter.dto;

public class GcpParametersDto {

    private GcpResourceEncryptionParametersDto gcpResourceEncryptionParametersDto;

    private GcpParametersDto(Builder builder) {
        gcpResourceEncryptionParametersDto = builder.gcpResourceEncryptionParametersDto;
    }

    public GcpResourceEncryptionParametersDto getGcpResourceEncryptionParametersDto() {
        return gcpResourceEncryptionParametersDto;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "GcpParametersDto{" +
                "gcpResourceEncryptionParametersDto=" + gcpResourceEncryptionParametersDto +
                "}";
    }

    public static final class Builder {
        private GcpResourceEncryptionParametersDto gcpResourceEncryptionParametersDto;

        public Builder withEncryptionParameters(GcpResourceEncryptionParametersDto gcpResourceEncryptionParametersDto) {
            this.gcpResourceEncryptionParametersDto = gcpResourceEncryptionParametersDto;
            return this;
        }

        public GcpParametersDto build() {
            return new GcpParametersDto(this);
        }
    }
}