package com.sequenceiq.environment.environment.validation.storagelocation;

import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;

public interface EnvironmentTelemetryLoggingStorageLocationValidator {

    void validate(String storageLocation, Environment environment, ValidationResultBuilder resultBuilder);

    CloudPlatform getCloudPlatform();
}
