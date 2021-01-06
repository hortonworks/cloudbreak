package com.sequenceiq.environment.parameter.dto.s3guard;

public interface S3GuardParameters {

    String getS3GuardTableName();

    S3GuardTableCreation getDynamoDbTableCreation();
}
