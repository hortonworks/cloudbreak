package com.sequenceiq.environment.environment.validation;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.validation.cloudstorage.EnvironmentBackupStorageConfigurationValidator;
import com.sequenceiq.environment.environment.validation.cloudstorage.EnvironmentBackupStorageLocationValidator;
import com.sequenceiq.environment.environment.validation.cloudstorage.EnvironmentLogStorageConfigurationValidator;
import com.sequenceiq.environment.environment.validation.cloudstorage.EnvironmentLogStorageLocationValidator;
import com.sequenceiq.environment.environment.validation.validators.EncryptionKeyArnValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentAuthenticationValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentNetworkProviderValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentParameterValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentRegionValidator;
import com.sequenceiq.environment.parameter.dto.ParametersDto;

@Service
public class EnvironmentFlowValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentFlowValidatorService.class);

    private final EnvironmentRegionValidator environmentRegionValidator;

    private final EnvironmentNetworkProviderValidator environmentNetworkProviderValidator;

    private final EnvironmentLogStorageLocationValidator logStorageLocationValidator;

    private final EnvironmentLogStorageConfigurationValidator logStorageConfigurationValidator;

    private final EnvironmentBackupStorageLocationValidator backupLocationValidator;

    private final EnvironmentBackupStorageConfigurationValidator backupStorageConfigurationValidator;

    private final EnvironmentParameterValidator environmentParameterValidator;

    private final EnvironmentAuthenticationValidator environmentAuthenticationValidator;

    private final EncryptionKeyArnValidator encryptionKeyArnValidator;

    public EnvironmentFlowValidatorService(
            EnvironmentRegionValidator environmentRegionValidator,
            EnvironmentNetworkProviderValidator environmentNetworkProviderValidator,
            EnvironmentLogStorageLocationValidator logStorageLocationValidator,
            EnvironmentBackupStorageLocationValidator backupLocationValidator,
            EnvironmentParameterValidator environmentParameterValidator,
            EnvironmentAuthenticationValidator environmentAuthenticationValidator,
            EnvironmentLogStorageConfigurationValidator logStorageConfigurationValidator,
            EnvironmentBackupStorageConfigurationValidator backupStorageConfigurationValidator,
            EncryptionKeyArnValidator encryptionKeyArnValidator) {
        this.environmentRegionValidator = environmentRegionValidator;
        this.environmentNetworkProviderValidator = environmentNetworkProviderValidator;
        this.logStorageLocationValidator = logStorageLocationValidator;
        this.backupLocationValidator = backupLocationValidator;
        this.environmentParameterValidator = environmentParameterValidator;
        this.environmentAuthenticationValidator = environmentAuthenticationValidator;
        this.logStorageConfigurationValidator = logStorageConfigurationValidator;
        this.backupStorageConfigurationValidator = backupStorageConfigurationValidator;
        this.encryptionKeyArnValidator = encryptionKeyArnValidator;
    }

    public ValidationResult.ValidationResultBuilder validateRegionsAndLocation(String location, Set<String> requestedRegions,
            Environment environment, CloudRegions cloudRegions) {
        LOGGER.debug("Validating regions and location for environment.");
        String cloudPlatform = environment.getCloudPlatform();
        ValidationResult.ValidationResultBuilder regionValidationResult
                = environmentRegionValidator.validateRegions(requestedRegions, cloudRegions, cloudPlatform);
        ValidationResult.ValidationResultBuilder locationValidationResult
                = environmentRegionValidator.validateLocation(location, requestedRegions, cloudRegions, cloudPlatform);
        return regionValidationResult.merge(locationValidationResult.build());
    }

    public ValidationResult validateTelemetryLoggingStorageLocation(Environment environment) {
        return logStorageLocationValidator.validateTelemetryLoggingStorageLocation(environment);
    }

    public ValidationResult validateTelemetryLoggingStorageConfig(Environment environment) {
        return logStorageConfigurationValidator.validateTelemetryLoggingStorageConfiguration(environment);
    }

    public ValidationResult validateBackupStorageLocation(Environment environment) {
        return backupLocationValidator.validateBackupStorageLocation(environment);
    }

    public ValidationResult validateBackupStorageConfig(Environment environment) {
        return backupStorageConfigurationValidator.validateBackupStorageConfiguration(environment);
    }

    public ValidationResult validateNetworkWithProvider(EnvironmentValidationDto environmentValidationDto) {
        return environmentNetworkProviderValidator.validate(environmentValidationDto);
    }

    public ValidationResult validateParameters(EnvironmentValidationDto environmentValidationDto, ParametersDto parametersDto) {
        return environmentParameterValidator.validate(environmentValidationDto, parametersDto);
    }

    public ValidationResult validateAuthentication(EnvironmentValidationDto environmentValidationDto) {
        return environmentAuthenticationValidator.validate(environmentValidationDto);
    }

    public ValidationResult validateAwsKeysPresent(EnvironmentValidationDto environmentValidationDto, ParametersDto parametersDto) {
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        if (environmentValidationDto.getEnvironmentDto().getCloudPlatform().equals("AWS")) {
            return encryptionKeyArnValidator.validate(environmentValidationDto, parametersDto);
        }
        return validationResultBuilder.build();
    }

}
