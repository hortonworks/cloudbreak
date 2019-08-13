package com.sequenceiq.environment.environment.validation.storagelocation;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;

@Component
public class EnvironmentLogStorageLocationValidator {

    @Inject
    private Map<CloudPlatform, EnvironmentTelemetryLoggingStorageLocationValidator> loggingStorageLocationValidatorsByCloudPlatform;

    public ValidationResult validateTelemetryLoggingStorageLocation(Environment environment) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        Optional.ofNullable(environment.getTelemetry())
                .map(EnvironmentTelemetry::getLogging)
                .map(EnvironmentLogging::getStorageLocation)
                .ifPresent(location -> isLogStorageInCorrectRegion(location, environment, resultBuilder));
        return resultBuilder.build();
    }

    private void isLogStorageInCorrectRegion(String storageLocation, Environment environment, ValidationResultBuilder resultBuilder) {
        EnvironmentTelemetryLoggingStorageLocationValidator loggingStorageLocationValidator =
                loggingStorageLocationValidatorsByCloudPlatform.get(CloudPlatform.valueOf(environment.getCloudPlatform()));
        if (loggingStorageLocationValidator != null) {
            loggingStorageLocationValidator.validate(storageLocation, environment, resultBuilder);
        } else {
            resultBuilder.error(String.format("Environment specific logging storage location is not supported for cloud platform: '%s'!",
                    environment.getCloudPlatform()));
        }
    }
}
