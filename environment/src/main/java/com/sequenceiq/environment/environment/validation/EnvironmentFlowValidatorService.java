package com.sequenceiq.environment.environment.validation;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.validation.cloudstorage.EnvironmentLogStorageLocationValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentNetworkProviderValidator;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.validation.validators.AwsParameterProcessor;

@Service
public class EnvironmentFlowValidatorService {

    private final EnvironmentNetworkProviderValidator environmentNetworkProviderValidator;

    private final EnvironmentLogStorageLocationValidator logStorageLocationValidator;

    private final AwsParameterProcessor awsParameterProcessor;

    public EnvironmentFlowValidatorService(
            EnvironmentNetworkProviderValidator environmentNetworkProviderValidator,
            EnvironmentLogStorageLocationValidator logStorageLocationValidator, AwsParameterProcessor awsParameterProcessor) {
        this.environmentNetworkProviderValidator = environmentNetworkProviderValidator;
        this.logStorageLocationValidator = logStorageLocationValidator;
        this.awsParameterProcessor = awsParameterProcessor;
    }

    public ValidationResult validateTelemetryLoggingStorageLocation(Environment environment) {
        return logStorageLocationValidator.validateTelemetryLoggingStorageLocation(environment);
    }

    public ValidationResult validateNetworkWithProvider(EnvironmentDto environmentDto) {
        return environmentNetworkProviderValidator.validate(environmentDto);
    }

    public ValidationResult processAwsParameters(EnvironmentDto environmentDto, ParametersDto parametersDto) {
        return awsParameterProcessor.processAwsParameters(environmentDto, parametersDto);
    }
}
