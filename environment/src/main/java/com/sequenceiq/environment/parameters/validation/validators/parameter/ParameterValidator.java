package com.sequenceiq.environment.parameters.validation.validators.parameter;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;

public interface ParameterValidator {

    ValidationResult validate(EnvironmentDto environmentDto, ParametersDto parametersDto, ValidationResultBuilder validationResultBuilder);

    CloudPlatform getcloudPlatform();
}
