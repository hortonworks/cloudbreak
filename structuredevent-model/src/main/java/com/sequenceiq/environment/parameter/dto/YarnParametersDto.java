package com.sequenceiq.environment.parameter.dto;

public class YarnParametersDto {

    private YarnParametersDto(Builder builder) {
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "YarnParametersDto{}";
    }

    public static final class Builder {

        private Builder() {
        }

        public YarnParametersDto build() {
            return new YarnParametersDto(this);
        }
    }
}
