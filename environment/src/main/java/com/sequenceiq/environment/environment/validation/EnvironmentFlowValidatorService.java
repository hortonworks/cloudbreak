package com.sequenceiq.environment.environment.validation;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.validation.cloudstorage.EnvironmentLogStorageLocationValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentAuthenticationValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentNetworkProviderValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentParameterValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentRegionValidator;
import com.sequenceiq.environment.parameters.dto.ParametersDto;

@Service
public class EnvironmentFlowValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentFlowValidatorService.class);

    private final EnvironmentRegionValidator environmentRegionValidator;

    private final EnvironmentNetworkProviderValidator environmentNetworkProviderValidator;

    private final EnvironmentLogStorageLocationValidator logStorageLocationValidator;

    private final EnvironmentParameterValidator environmentParameterValidator;

    private final EnvironmentAuthenticationValidator environmentAuthenticationValidator;

    public EnvironmentFlowValidatorService(
            EnvironmentRegionValidator environmentRegionValidator, EnvironmentNetworkProviderValidator environmentNetworkProviderValidator,
            EnvironmentLogStorageLocationValidator logStorageLocationValidator, EnvironmentParameterValidator environmentParameterValidator,
            EnvironmentAuthenticationValidator environmentAuthenticationValidator) {
        this.environmentRegionValidator = environmentRegionValidator;
        this.environmentNetworkProviderValidator = environmentNetworkProviderValidator;
        this.logStorageLocationValidator = logStorageLocationValidator;
        this.environmentParameterValidator = environmentParameterValidator;
        this.environmentAuthenticationValidator = environmentAuthenticationValidator;
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

    public ValidationResult validateNetworkWithProvider(EnvironmentDto environmentDto) {
        return environmentNetworkProviderValidator.validate(environmentDto);
    }

    public ValidationResult validateParameters(EnvironmentDto environmentDto, ParametersDto parametersDto) {
        return environmentParameterValidator.validate(environmentDto, parametersDto);
    }

    public ValidationResult validateAuthentication(EnvironmentDto environmentDto) {
        return environmentAuthenticationValidator.validate(environmentDto);
    }

}
