package com.sequenceiq.environment.api.v1.environment.model.common;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class AwsParams {

    @ApiModelProperty(EnvironmentModelDescription.S3_GUARD)
    private S3Guard s3Guard;

    public AwsParams() {
        this.s3Guard = new S3Guard();
    }

    public S3Guard getS3Guard() {
        return s3Guard;
    }

    public void setS3Guard(S3Guard s3Guard) {
        this.s3Guard = s3Guard;
    }

    public static class S3Guard {

        @ApiModelProperty(EnvironmentModelDescription.S3_GUARD_DYNAMO_TABLE)
        private String dynamoTableName;

        public String getDynamoTableName() {
            return dynamoTableName;
        }

        public void setDynamoTableName(String dynamoTableName) {
            this.dynamoTableName = dynamoTableName;
        }
    }
}
