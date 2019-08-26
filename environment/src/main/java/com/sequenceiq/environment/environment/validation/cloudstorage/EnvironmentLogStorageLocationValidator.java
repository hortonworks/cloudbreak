package com.sequenceiq.environment.environment.validation.cloudstorage;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;

@Component
public class EnvironmentLogStorageLocationValidator {

    private final CloudStorageLocationValidator cloudStorageLocationValidator;

    public EnvironmentLogStorageLocationValidator(CloudStorageLocationValidator cloudStorageLocationValidator) {
        this.cloudStorageLocationValidator = cloudStorageLocationValidator;
    }

    public ValidationResult validateTelemetryLoggingStorageLocation(String userCrn, Environment environment) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        Optional.ofNullable(environment.getTelemetry())
                .map(EnvironmentTelemetry::getLogging)
                .map(EnvironmentLogging::getStorageLocation)
                .ifPresent(location -> cloudStorageLocationValidator.validate(userCrn, location, environment, resultBuilder));
        return resultBuilder.build();
    }
}
