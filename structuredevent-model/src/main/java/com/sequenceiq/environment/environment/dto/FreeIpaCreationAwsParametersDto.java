package com.sequenceiq.environment.environment.dto;

public class FreeIpaCreationAwsParametersDto {

    private FreeIpaCreationAwsSpotParametersDto spot;

    private FreeIpaCreationAwsParametersDto() {
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

    public static class Builder {

        private FreeIpaCreationAwsSpotParametersDto spot;

        public Builder withSpot(FreeIpaCreationAwsSpotParametersDto spot) {
            this.spot = spot;
            return this;
        }

        public FreeIpaCreationAwsParametersDto build() {
            FreeIpaCreationAwsParametersDto response = new FreeIpaCreationAwsParametersDto();
            response.spot = spot;
            return response;
        }

    }
}
