package com.sequenceiq.environment.parameters.validation.validators.parameter;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;

@Component
public class AwsParameterValidator implements ParameterValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsParameterValidator.class);

    private static final int PERCENTAGE_MIN = 0;

    private static final int PERCENTAGE_MAX = 100;

    private final ParametersService parametersService;

    public AwsParameterValidator(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    @Override
    public ValidationResult validate(EnvironmentValidationDto environmentValidationDto, ParametersDto parametersDto,
            ValidationResultBuilder validationResultBuilder) {
        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        LOGGER.debug("ParametersDto: {}", parametersDto);
        AwsParametersDto awsParametersDto = parametersDto.getAwsParametersDto();
        if (Objects.isNull(awsParametersDto)) {
            LOGGER.debug("No aws parameters defined.");
            return validationResultBuilder.build();
        }

        if (awsParametersDto.getFreeIpaSpotPercentage() < PERCENTAGE_MIN || awsParametersDto.getFreeIpaSpotPercentage() > PERCENTAGE_MAX) {
            validationResultBuilder.error(String.format("FreeIpa spot percentage must be between %d and %d.", PERCENTAGE_MIN, PERCENTAGE_MAX));
        }
        return validationResultBuilder.build();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}

