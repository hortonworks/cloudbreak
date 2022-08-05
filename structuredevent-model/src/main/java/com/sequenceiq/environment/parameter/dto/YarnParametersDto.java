package com.sequenceiq.environment.parameter.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = YarnParametersDto.Builder.class)
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

    @JsonPOJOBuilder
    public static final class Builder {

        private Builder() {
        }

        public YarnParametersDto build() {
            return new YarnParametersDto(this);
        }
    }
}
