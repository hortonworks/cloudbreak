package com.sequenceiq.environment.environment.validation.cloudstorage;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;

@Component
public class EnvironmentBackupStorageConfigurationValidator extends EnvironmentStorageConfigurationValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentBackupStorageConfigurationValidator.class);

    /**
     * Validate backup related storage configuration.
     */
    public ValidationResult validateBackupStorageConfiguration(Environment environment) {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        Optional.ofNullable(environment.getBackup())
                .filter(backup -> isCloudStorageEnabled(backup))
                .ifPresent(backup -> validate(environment, resultBuilder));
        return resultBuilder.build();
    }

    private boolean isCloudStorageEnabled(EnvironmentBackup backup) {
        return backup.getS3() != null || backup.getAdlsGen2() != null || backup.getGcs() != null;
    }

    private void validate(Environment environment, ValidationResult.ValidationResultBuilder resultBuilder) {
        EnvironmentBackup backup = environment.getBackup();
        if (backup.getS3() != null && environment.getCloudPlatform().equals(CloudConstants.AWS)) {
            LOGGER.debug("Validate S3 backup storage configuration for {} environment", environment.getId());
            validateS3Config(environment, resultBuilder, backup.getS3().getInstanceProfile());
        } else if (backup.getAdlsGen2() != null && environment.getCloudPlatform().equals(CloudConstants.AZURE)) {
            LOGGER.debug("Validate Adls Gen2 backup storage configuration for {} environment", environment.getId());
            validateAdlsGen2Config(environment, resultBuilder, backup.getAdlsGen2().getManagedIdentity());
        } else if (backup.getGcs() != null && environment.getCloudPlatform().equals(CloudConstants.GCP)) {
            LOGGER.debug("Validate GCS backup storage configuration for {} environment", environment.getId());
            validateGcsConfig(environment, resultBuilder, backup.getGcs().getServiceAccountEmail());
        }
    }
}
