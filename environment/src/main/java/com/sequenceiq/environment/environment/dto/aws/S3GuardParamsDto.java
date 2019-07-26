package com.sequenceiq.environment.environment.dto.aws;

public class S3GuardParamsDto {
    private String dynamoDbTableName;

    public S3GuardParamsDto() {
    }

    private S3GuardParamsDto(Builder builder) {
        this.dynamoDbTableName = builder.dynamoDbTableName;
    }

    public String getDynamoDbTableName() {
        return dynamoDbTableName;
    }

    public static Builder aS3GuardParamsBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String dynamoDbTableName;

        public Builder withDynamoDbTableName(final String dynamoDbTableName) {
            this.dynamoDbTableName = dynamoDbTableName;
            return this;
        }

        public S3GuardParamsDto build() {
            return new S3GuardParamsDto(this);
        }
    }
}
