package com.sequenceiq.datalake.service.validation.cloudstorage;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

public interface CloudStorageLocationValidator {

    void validate(String storageLocation, DetailedEnvironmentResponse environment, ValidationResultBuilder resultBuilder);

    CloudPlatform getCloudPlatform();
}
