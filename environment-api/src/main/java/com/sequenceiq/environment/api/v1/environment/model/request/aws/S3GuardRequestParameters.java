package com.sequenceiq.environment.api.v1.environment.model.request.aws;

import java.io.Serializable;

import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Deprecated
@Schema(name = "S3GuardV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
public class S3GuardRequestParameters implements Serializable {

    @Deprecated
    @Pattern(regexp = "^[a-zA-Z0-9\\.\\-_]{3,255}$",
            message = "Table name must be between 3 and 255 characters long and can contain only alpnahumeric charachters, dot, dash and hyphen.")
    @Schema(description = EnvironmentModelDescription.S3_GUARD_DYNAMO_TABLE_NAME)
    private String dynamoDbTableName;

    public S3GuardRequestParameters() {
    }

    private S3GuardRequestParameters(Builder builder) {
        this.dynamoDbTableName = builder.dynamoDbTableName;
    }

    @Deprecated
    public String getDynamoDbTableName() {
        return dynamoDbTableName;
    }

    @Deprecated
    public void setDynamoDbTableName(String dynamoDbTableName) {
        this.dynamoDbTableName = dynamoDbTableName;
    }

    @Override
    public String toString() {
        return "S3GuardRequestParameters{" +
                "dynamoDbTableName='" + dynamoDbTableName + '\'' +
                '}';
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

        public S3GuardRequestParameters build() {
            return new S3GuardRequestParameters(this);
        }
    }
}
