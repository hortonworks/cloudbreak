package com.sequenceiq.environment.environment.validation.cloudstorage;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;

@Component
public class EnvironmentLogStorageConfigurationValidator extends EnvironmentStorageConfigurationValidator {

    /**
     * Validate telemetry related logging storage configuration.
     * Currently, filter out cloudwatch (or any other cloud logging service) related validations
     */
    public ValidationResult validateTelemetryLoggingStorageConfiguration(Environment environment) {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        Optional.ofNullable(environment.getTelemetry())
                .map(EnvironmentTelemetry::getLogging)
                .filter(logging -> isCloudStorageEnabled(logging))
                .ifPresent(location -> validate(environment, resultBuilder));
        return resultBuilder.build();
    }

    protected boolean isCloudStorageEnabled(EnvironmentLogging logging) {
        return logging.getS3() != null || logging.getAdlsGen2() != null || logging.getGcs() != null;
    }

    private void validate(Environment environment, ValidationResult.ValidationResultBuilder resultBuilder) {
        EnvironmentLogging logging = environment.getTelemetry().getLogging();
        if (logging.getS3() != null && environment.getCloudPlatform().equals(CloudConstants.AWS)) {
            validateS3Config(environment, resultBuilder, logging.getS3().getInstanceProfile());
        } else if (logging.getAdlsGen2() != null && environment.getCloudPlatform().equals(CloudConstants.AZURE)) {
            validateAdlsGen2Config(environment, resultBuilder, logging.getAdlsGen2().getManagedIdentity());
        } else if (logging.getGcs() != null && environment.getCloudPlatform().equals(CloudConstants.GCP)) {
            validateGcsConfig(environment, resultBuilder, logging.getGcs().getServiceAccountEmail());
        }
    }
}
