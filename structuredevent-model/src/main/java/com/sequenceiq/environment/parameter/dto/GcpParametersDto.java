package com.sequenceiq.environment.parameter.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = GcpParametersDto.Builder.class)
public class GcpParametersDto {

    private final GcpResourceEncryptionParametersDto gcpResourceEncryptionParametersDto;

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

    @JsonPOJOBuilder
    public static final class Builder {
        private GcpResourceEncryptionParametersDto gcpResourceEncryptionParametersDto;

        public Builder withGcpResourceEncryptionParametersDto(GcpResourceEncryptionParametersDto gcpResourceEncryptionParametersDto) {
            this.gcpResourceEncryptionParametersDto = gcpResourceEncryptionParametersDto;
            return this;
        }

        public GcpParametersDto build() {
            return new GcpParametersDto(this);
        }
    }
}
