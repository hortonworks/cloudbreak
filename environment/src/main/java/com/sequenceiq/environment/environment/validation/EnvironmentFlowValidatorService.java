package com.sequenceiq.environment.environment.validation;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.validation.cloudstorage.EnvironmentLogStorageLocationValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentNetworkProviderValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentParameterValidator;
import com.sequenceiq.environment.parameters.dto.ParametersDto;

@Service
public class EnvironmentFlowValidatorService {

    private final EnvironmentNetworkProviderValidator environmentNetworkProviderValidator;

    private final EnvironmentLogStorageLocationValidator logStorageLocationValidator;

    private final EnvironmentParameterValidator environmentParameterValidator;

    public EnvironmentFlowValidatorService(
            EnvironmentNetworkProviderValidator environmentNetworkProviderValidator,
            EnvironmentLogStorageLocationValidator logStorageLocationValidator, EnvironmentParameterValidator environmentParameterValidator) {
        this.environmentNetworkProviderValidator = environmentNetworkProviderValidator;
        this.logStorageLocationValidator = logStorageLocationValidator;
        this.environmentParameterValidator = environmentParameterValidator;
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

}
