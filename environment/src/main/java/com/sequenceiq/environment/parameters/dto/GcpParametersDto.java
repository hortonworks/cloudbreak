package com.sequenceiq.environment.parameters.dto;

public class GcpParametersDto {

    private GcpParametersDto(Builder builder) {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Builder() {

        }

        public GcpParametersDto build() {
            return new GcpParametersDto(this);
        }
    }
}
