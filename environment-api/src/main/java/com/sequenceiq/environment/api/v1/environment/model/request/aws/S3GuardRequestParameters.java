package com.sequenceiq.environment.api.v1.environment.model.request.aws;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "S3GuardV1Parameters")
public class S3GuardRequestParameters {

    private String dynamoDbTableName;

    public S3GuardRequestParameters() {
    }

    private S3GuardRequestParameters(Builder builder) {
        this.dynamoDbTableName = builder.dynamoDbTableName;
    }

    public String getDynamoDbTableName() {
        return dynamoDbTableName;
    }

    public void setDynamoDbTableName(String dynamoDbTableName) {
        this.dynamoDbTableName = dynamoDbTableName;
    }

    public static Builder s3GuardRequestParameters() {
        return new Builder();
    }

    public static final class Builder {
        private String dynamoDbTableName;

        public Builder withDynamoDbTableName(final String dynamoDbTableName) {
            this.dynamoDbTableName = dynamoDbTableName;
            return this;
        }

        public S3GuardRequestParameters build() {
            return new S3GuardRequestParameters(this);
        }
    }
}
