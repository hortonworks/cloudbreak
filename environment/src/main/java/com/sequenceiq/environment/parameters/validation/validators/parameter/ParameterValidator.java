package com.sequenceiq.environment.parameters.validation.validators.parameter;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;

public interface ParameterValidator {

    ValidationResult validate(EnvironmentValidationDto environmentValidationDto, ParametersDto parametersDto, ValidationResultBuilder validationResultBuilder);

    CloudPlatform getcloudPlatform();
}
