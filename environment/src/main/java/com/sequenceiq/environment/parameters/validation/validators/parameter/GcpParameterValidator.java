package com.sequenceiq.environment.parameters.validation.validators.parameter;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.parameter.dto.GcpParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;

@Component
public class GcpParameterValidator implements ParameterValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpParameterValidator.class);

    @Override
    public ValidationResult validate(EnvironmentValidationDto environmentValidationDto, ParametersDto parametersDto,
            ValidationResultBuilder validationResultBuilder) {

        LOGGER.debug("ParametersDto: {}", parametersDto);
        GcpParametersDto gcpParametersDto = parametersDto.getGcpParametersDto();
        if (Objects.isNull(gcpParametersDto)) {
            return validationResultBuilder.build();
        }
        return validationResultBuilder.build();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }
}