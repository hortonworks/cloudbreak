package com.sequenceiq.environment.environment.dto;

public class FreeIpaCreationAwsSpotParametersDto {

    private final Integer percentage;

    private final Double maxPrice;

    private FreeIpaCreationAwsSpotParametersDto(Builder builder) {
        percentage = builder.percentage;
        maxPrice = builder.maxPrice;
    }

    public Integer getPercentage() {
        return percentage;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "FreeIpaCreationAwsSpotParametersDto{" +
                "percentage=" + percentage + ", " +
                "maxPrice=" + maxPrice +
                '}';
    }

    public static class Builder {

        private Integer percentage;

        private Double maxPrice;

        private Builder() {
        }

        public Builder withPercentage(int percentage) {
            this.percentage = percentage;
            return this;
        }

        public Builder withMaxPrice(Double maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }

        public FreeIpaCreationAwsSpotParametersDto build() {
            return new FreeIpaCreationAwsSpotParametersDto(this);
        }
    }
}
