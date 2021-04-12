package com.sequenceiq.environment.environment.validation.cloudstorage;

import java.util.Optional;

import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;

@Component
public class EnvironmentBackupStorageLocationValidator {

    private final CloudStorageLocationValidator cloudStorageLocationValidator;

    public EnvironmentBackupStorageLocationValidator(CloudStorageLocationValidator cloudStorageLocationValidator) {
        this.cloudStorageLocationValidator = cloudStorageLocationValidator;
    }

    /**
     * Validate backup storage location.
     * Currently, filter out cloudwatch (or any other cloud logging service) related validations
     */
    public ValidationResult validateBackupStorageLocation(Environment environment) {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        Optional.ofNullable(environment.getBackup())
                .filter(backup -> isCloudStorageEnabled(backup) && backup.getCloudwatch() == null)
                .map(EnvironmentBackup::getStorageLocation)
                .ifPresent(location -> cloudStorageLocationValidator.validateBackup(location, environment, resultBuilder));
        return resultBuilder.build();
    }

    private boolean isCloudStorageEnabled(EnvironmentBackup backup) {
        return backup.getS3() != null || backup.getAdlsGen2() != null || backup.getGcs() != null;
    }
}
