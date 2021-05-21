package com.sequenceiq.environment.environment.validation.validators;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameters.validation.validators.parameter.ParameterValidator;

@Component
public class EnvironmentParameterValidator {

    private final Map<String, ParameterValidator> parameterValidatorsByCloudPlatform;

    public EnvironmentParameterValidator(List<ParameterValidator> parameterValidators) {
        parameterValidatorsByCloudPlatform = parameterValidators
                .stream()
                .collect(Collectors.toMap(pv -> pv.getcloudPlatform().name(), Function.identity()));
    }

    public ValidationResult validate(EnvironmentValidationDto environmentValidationDto, ParametersDto parametersDto) {
        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        if (Objects.isNull(parametersDto)) {
            return ValidationResult.empty();
        }
        ParameterValidator parameterValidator = parameterValidatorsByCloudPlatform.get(environmentDto.getCloudPlatform());
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        return parameterValidator != null
                ? parameterValidator.validate(environmentValidationDto, parametersDto, resultBuilder)
                : resultBuilder
                .error(String.format("Environment specific parameter is not supported for cloud platform: '%s'!", environmentDto.getCloudPlatform())).build();
    }
}
