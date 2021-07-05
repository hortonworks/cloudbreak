package com.sequenceiq.environment.parameter.dto;

public class GcpParametersDto {

    private GcpParametersDto(Builder builder) {
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "GcpParametersDto{}";
    }

    public static final class Builder {

        private Builder() {

        }

        public GcpParametersDto build() {
            return new GcpParametersDto(this);
        }
    }
}
