package com.sequenceiq.environment.parameters.dto;

import com.sequenceiq.environment.parameters.dto.s3guard.S3GuardParameters;

public class AwsParametersDto implements S3GuardParameters {

    private final String dynamoDbTableName;

    private AwsParametersDto(Builder builder) {
        dynamoDbTableName = builder.dynamoDbTableName;
    }

    @Override
    public String getS3GuardTableName() {
        return dynamoDbTableName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String dynamoDbTableName;

        private Builder() {
        }

        public Builder withDynamoDbTableName(String dynamoDbTableName) {
            this.dynamoDbTableName = dynamoDbTableName;
            return this;
        }

        public AwsParametersDto build() {
            return new AwsParametersDto(this);
        }
    }
}
