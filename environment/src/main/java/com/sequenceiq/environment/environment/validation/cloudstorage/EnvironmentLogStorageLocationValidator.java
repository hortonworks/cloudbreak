package com.sequenceiq.environment.environment.validation.cloudstorage;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;

@Component
public class EnvironmentLogStorageLocationValidator {

    private final CloudStorageLocationValidator cloudStorageLocationValidator;

    public EnvironmentLogStorageLocationValidator(CloudStorageLocationValidator cloudStorageLocationValidator) {
        this.cloudStorageLocationValidator = cloudStorageLocationValidator;
    }

    /**
     * Validate telemetry related logging storage location.
     * Currently, filter out cloudwatch (or any other cloud logging service) related validations
     */
    public ValidationResult validateTelemetryLoggingStorageLocation(Environment environment) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        Optional.ofNullable(environment.getTelemetry())
                .map(EnvironmentTelemetry::getLogging)
                .filter(this::isCloudStorageEnabled)
                .map(EnvironmentLogging::getStorageLocation)
                .ifPresent(location -> cloudStorageLocationValidator.validate(location, environment, resultBuilder));
        return resultBuilder.build();
    }

    private boolean isCloudStorageEnabled(EnvironmentLogging logging) {
        return logging.getS3() != null || logging.getAdlsGen2() != null || logging.getGcs() != null;
    }
}
