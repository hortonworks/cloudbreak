package com.sequenceiq.environment.parameters.dto;

import com.sequenceiq.environment.parameters.dao.domain.S3GuardTableCreation;
import com.sequenceiq.environment.parameters.dto.s3guard.S3GuardParameters;

public class AwsParametersDto implements S3GuardParameters {

    private final String dynamoDbTableName;

    private S3GuardTableCreation dynamoDbTableCreation;

    private int freeIpaSpotPercentage;

    private Double freeIpaSpotMaxPrice;

    private AwsParametersDto(Builder builder) {
        dynamoDbTableName = builder.dynamoDbTableName;
        dynamoDbTableCreation = builder.dynamoDbTableCreation;
        freeIpaSpotPercentage = builder.freeIpaSpotPercentage;
        freeIpaSpotMaxPrice = builder.freeIpaSpotMaxPrice;
    }

    @Override
    public String getS3GuardTableName() {
        return dynamoDbTableName;
    }

    @Override
    public S3GuardTableCreation getDynamoDbTableCreation() {
        return dynamoDbTableCreation;
    }

    public void setDynamoDbTableCreation(S3GuardTableCreation dynamoDbTableCreation) {
        this.dynamoDbTableCreation = dynamoDbTableCreation;
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

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AwsParametersDto{"
                + "dynamoDbTableName='" + dynamoDbTableName + '\''
                + ", dynamoDbTableCreation=" + dynamoDbTableCreation
                + ", freeIpaSpotPercentage=" + freeIpaSpotPercentage
                + ", freeIpaSpotMaxPrice=" + freeIpaSpotMaxPrice
                + '}';
    }

    public static final class Builder {

        private String dynamoDbTableName;

        private S3GuardTableCreation dynamoDbTableCreation;

        private int freeIpaSpotPercentage;

        private Double freeIpaSpotMaxPrice;

        private Builder() {
        }

        public Builder withDynamoDbTableName(String dynamoDbTableName) {
            this.dynamoDbTableName = dynamoDbTableName;
            return this;
        }

        public Builder withDynamoDbTableCreation(S3GuardTableCreation dynamoDbTableCreation) {
            this.dynamoDbTableCreation = dynamoDbTableCreation;
            return this;
        }

        public Builder withFreeIpaSpotPercentage(int freeIpaSpotPercentage) {
            this.freeIpaSpotPercentage = freeIpaSpotPercentage;
            return this;
        }

        public Builder withFreeIpaSpotMaxPrice(Double freeIpaSpotMaxPrice) {
            this.freeIpaSpotMaxPrice = freeIpaSpotMaxPrice;
            return this;
        }

        public AwsParametersDto build() {
            return new AwsParametersDto(this);
        }
    }
}
