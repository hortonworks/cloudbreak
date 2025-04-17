package com.sequenceiq.environment.environment.validation;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.validation.cloudstorage.EnvironmentBackupStorageConfigurationValidator;
import com.sequenceiq.environment.environment.validation.cloudstorage.EnvironmentBackupStorageLocationValidator;
import com.sequenceiq.environment.environment.validation.cloudstorage.EnvironmentLogStorageConfigurationValidator;
import com.sequenceiq.environment.environment.validation.cloudstorage.EnvironmentLogStorageLocationValidator;
import com.sequenceiq.environment.environment.validation.validators.EncryptionKeyArnValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentAuthenticationValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentComputeClusterCredentialValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentDataServicesValidator;
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

    private final EnvironmentDataServicesValidator environmentDataServicesValidator;

    private final EnvironmentComputeClusterCredentialValidator environmentComputeClusterCredentialValidator;

    public EnvironmentFlowValidatorService(
            EnvironmentRegionValidator environmentRegionValidator,
            EnvironmentNetworkProviderValidator environmentNetworkProviderValidator,
            EnvironmentLogStorageLocationValidator logStorageLocationValidator,
            EnvironmentBackupStorageLocationValidator backupLocationValidator,
            EnvironmentParameterValidator environmentParameterValidator,
            EnvironmentAuthenticationValidator environmentAuthenticationValidator,
            EnvironmentLogStorageConfigurationValidator logStorageConfigurationValidator,
            EnvironmentBackupStorageConfigurationValidator backupStorageConfigurationValidator,
            EncryptionKeyArnValidator encryptionKeyArnValidator,
            EnvironmentDataServicesValidator environmentDataServicesValidator,
            EnvironmentComputeClusterCredentialValidator environmentComputeClusterCredentialValidator) {
        this.environmentRegionValidator = environmentRegionValidator;
        this.environmentNetworkProviderValidator = environmentNetworkProviderValidator;
        this.logStorageLocationValidator = logStorageLocationValidator;
        this.backupLocationValidator = backupLocationValidator;
        this.environmentParameterValidator = environmentParameterValidator;
        this.environmentAuthenticationValidator = environmentAuthenticationValidator;
        this.logStorageConfigurationValidator = logStorageConfigurationValidator;
        this.backupStorageConfigurationValidator = backupStorageConfigurationValidator;
        this.encryptionKeyArnValidator = encryptionKeyArnValidator;
        this.environmentDataServicesValidator = environmentDataServicesValidator;
        this.environmentComputeClusterCredentialValidator = environmentComputeClusterCredentialValidator;
    }

    public ValidationResult.ValidationResultBuilder validateRegionsAndLocation(String location, Set<String> requestedRegions,
            Environment environment, CloudRegions cloudRegions) {
        LOGGER.debug("Validate Environment cloud region and location which is {}.", cloudRegions);
        String cloudPlatform = environment.getCloudPlatform();
        ValidationResult.ValidationResultBuilder regionValidationResult
                = environmentRegionValidator.validateRegions(requestedRegions, cloudRegions, cloudPlatform);
        ValidationResult.ValidationResultBuilder locationValidationResult
                = environmentRegionValidator.validateLocation(location, requestedRegions, cloudRegions, cloudPlatform);
        return regionValidationResult.merge(locationValidationResult.build());
    }

    public ValidationResult validateTelemetryLoggingStorageLocation(Environment environment) {
        LOGGER.debug("Validate Environment telemetry location.");
        return logStorageLocationValidator.validateTelemetryLoggingStorageLocation(environment);
    }

    public ValidationResult validateTelemetryLoggingStorageConfig(Environment environment) {
        LOGGER.debug("Validate Environment telemetry configurations.");
        return logStorageConfigurationValidator.validateTelemetryLoggingStorageConfiguration(environment);
    }

    public ValidationResult validateBackupStorageLocation(Environment environment) {
        LOGGER.debug("Validate Environment backup location.");
        return backupLocationValidator.validateBackupStorageLocation(environment);
    }

    public ValidationResult validateBackupStorageConfig(Environment environment) {
        LOGGER.debug("Validate Environment backup configurations.");
        return backupStorageConfigurationValidator.validateBackupStorageConfiguration(environment);
    }

    public ValidationResult validateNetworkWithProvider(EnvironmentValidationDto environmentValidationDto) {
        LOGGER.debug("Validate Environment network configurations.");
        return environmentNetworkProviderValidator.validate(environmentValidationDto);
    }

    public ValidationResult validateParameters(EnvironmentValidationDto environmentValidationDto, ParametersDto parametersDto) {
        LOGGER.debug("Validate Environment parameters.");
        return environmentParameterValidator.validate(environmentValidationDto, parametersDto);
    }

    public ValidationResult validateAuthentication(EnvironmentValidationDto environmentValidationDto) {
        LOGGER.debug("Validate Environment authentication.");
        return environmentAuthenticationValidator.validate(environmentValidationDto);
    }

    public ValidationResult validateAwsKeysPresent(EnvironmentValidationDto environmentValidationDto) {
        LOGGER.debug("Validate Environment AWS keys.");
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        if (environmentValidationDto.getEnvironmentDto().getCloudPlatform().equals(CloudPlatform.AWS.name())) {
            return encryptionKeyArnValidator.validate(environmentValidationDto, true);
        }
        return validationResultBuilder.build();
    }

    public ValidationResult validateEnvironmentDataServices(EnvironmentValidationDto environmentValidationDto) {
        LOGGER.debug("Validate Environment data services");
        return environmentDataServicesValidator.validate(environmentValidationDto);
    }

    public ValidationResult validateCredentialForExternalizedComputeCluster(EnvironmentValidationDto environmentValidationDto) {
        LOGGER.debug("Validate Credential for externalized compute cluster");
        return environmentComputeClusterCredentialValidator.validate(environmentValidationDto);
    }
}
