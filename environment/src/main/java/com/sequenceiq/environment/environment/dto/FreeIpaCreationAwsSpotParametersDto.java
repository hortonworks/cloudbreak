package com.sequenceiq.environment.environment.dto;

public class FreeIpaCreationAwsSpotParametersDto {

    private Integer percentage;

    private FreeIpaCreationAwsSpotParametersDto() {
    }

    public Integer getPercentage() {
        return percentage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer percentage;

        public Builder withPercentage(int percentage) {
            this.percentage = percentage;
            return this;
        }

        public FreeIpaCreationAwsSpotParametersDto build() {
            FreeIpaCreationAwsSpotParametersDto response = new FreeIpaCreationAwsSpotParametersDto();
            response.percentage = percentage;
            return response;
        }
    }
}
