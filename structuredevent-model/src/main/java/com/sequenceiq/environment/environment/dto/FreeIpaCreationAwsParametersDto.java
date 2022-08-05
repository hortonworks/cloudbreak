package com.sequenceiq.environment.environment.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = FreeIpaCreationAwsParametersDto.Builder.class)
public class FreeIpaCreationAwsParametersDto {

    private final FreeIpaCreationAwsSpotParametersDto spot;

    private FreeIpaCreationAwsParametersDto(Builder builder) {
        spot = builder.spot;
    }

    public FreeIpaCreationAwsSpotParametersDto getSpot() {
        return spot;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "FreeIpaCreationAwsParametersDto{" +
                "spot=" + spot +
                '}';
    }

    @JsonPOJOBuilder
    public static class Builder {

        private FreeIpaCreationAwsSpotParametersDto spot;

        private Builder() {
        }

        public Builder withSpot(FreeIpaCreationAwsSpotParametersDto spot) {
            this.spot = spot;
            return this;
        }

        public FreeIpaCreationAwsParametersDto build() {
            return new FreeIpaCreationAwsParametersDto(this);
        }

    }
}
