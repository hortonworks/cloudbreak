package com.sequenceiq.environment.environment.validation.validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.dto.dataservices.CustomDockerRegistryParameters;
import com.sequenceiq.environment.environment.dto.dataservices.EnvironmentDataServices;

@Service
public class EnvironmentDataServicesValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDataServicesValidator.class);

    public ValidationResult validate(EnvironmentValidationDto environmentValidationDto) {
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        EnvironmentDataServices dataServices = environmentValidationDto.getEnvironmentDto().getDataServices();
        if (dataServices != null) {
            CustomDockerRegistryParameters customDockerRegistryParameters = dataServices.customDockerRegistry();
            if (customDockerRegistryParameters != null) {
                LOGGER.debug("Validate custom docker registry '{}'", customDockerRegistryParameters);
            }
        }
        return validationResultBuilder.build();
    }
}
