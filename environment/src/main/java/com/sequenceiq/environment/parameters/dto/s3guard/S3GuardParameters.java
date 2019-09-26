package com.sequenceiq.environment.parameters.dto.s3guard;

import com.sequenceiq.environment.parameters.dao.domain.S3GuardTableCreation;

public interface S3GuardParameters {

    String getS3GuardTableName();

    S3GuardTableCreation getDynamoDbTableCreation();
}
