package com.sequenceiq.environment.parameter.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = AwsParametersDto.Builder.class)
public class AwsParametersDto {

    private int freeIpaSpotPercentage;

    private Double freeIpaSpotMaxPrice;

    private final AwsDiskEncryptionParametersDto awsDiskEncryptionParametersDto;

    private AwsParametersDto(Builder builder) {
        freeIpaSpotPercentage = builder.freeIpaSpotPercentage;
        freeIpaSpotMaxPrice = builder.freeIpaSpotMaxPrice;
        awsDiskEncryptionParametersDto = builder.awsDiskEncryptionParametersDto;
    }

    public int getFreeIpaSpotPercentage() {
        return freeIpaSpotPercentage;
    }

    public void setFreeIpaSpotPercentage(int freeIpaSpotPercentage) {
        this.freeIpaSpotPercentage = freeIpaSpotPercentage;
    }

    public Double getFreeIpaSpotMaxPrice() {
        return freeIpaSpotMaxPrice;
    }

    public void setFreeIpaSpotMaxPrice(Double freeIpaSpotMaxPrice) {
        this.freeIpaSpotMaxPrice = freeIpaSpotMaxPrice;
    }

    public AwsDiskEncryptionParametersDto getAwsDiskEncryptionParametersDto() {
        return awsDiskEncryptionParametersDto;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AwsParametersDto{"
                + "freeIpaSpotPercentage='" + freeIpaSpotPercentage + '\''
                + ", freeIpaSpotMaxPrice=" + freeIpaSpotMaxPrice
                + ", awsDiskEncryptionParametersDto" + awsDiskEncryptionParametersDto
                + '}';
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private int freeIpaSpotPercentage;

        private Double freeIpaSpotMaxPrice;

        private AwsDiskEncryptionParametersDto awsDiskEncryptionParametersDto;

        private Builder() {
        }

        public Builder withFreeIpaSpotPercentage(int freeIpaSpotPercentage) {
            this.freeIpaSpotPercentage = freeIpaSpotPercentage;
            return this;
        }

        public Builder withFreeIpaSpotMaxPrice(Double freeIpaSpotMaxPrice) {
            this.freeIpaSpotMaxPrice = freeIpaSpotMaxPrice;
            return this;
        }

        public Builder withAwsDiskEncryptionParametersDto(AwsDiskEncryptionParametersDto awsDiskEncryptionParametersDto) {
            this.awsDiskEncryptionParametersDto = awsDiskEncryptionParametersDto;
            return this;
        }

        public AwsParametersDto build() {
            return new AwsParametersDto(this);
        }
    }
}
