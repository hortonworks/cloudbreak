package com.sequenceiq.environment.environment.validation.validators;

import static java.lang.String.format;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.client.thunderhead.computeapi.ThunderheadComputeApiService;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.dto.dataservices.CustomDockerRegistryParameters;
import com.sequenceiq.environment.environment.dto.dataservices.EnvironmentDataServices;

@Service
public class EnvironmentDataServicesValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDataServicesValidator.class);

    @Inject
    private ThunderheadComputeApiService thunderheadComputeApiService;

    @Inject
    private ManagedIdentityRoleValidator managedIdentityRoleValidator;

    public ValidationResult validate(EnvironmentValidationDto environmentValidationDto) {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        EnvironmentDataServices dataServices = environmentValidationDto.getEnvironmentDto().getDataServices();
        if (dataServices != null) {
            CustomDockerRegistryParameters customDockerRegistryParameters = dataServices.customDockerRegistry();
            if (customDockerRegistryParameters != null) {
                LOGGER.debug("Validate custom docker registry '{}'", customDockerRegistryParameters);
                boolean customDockerRegistryDescribable = thunderheadComputeApiService.customConfigDescribable(customDockerRegistryParameters);
                if (!customDockerRegistryDescribable) {
                    resultBuilder.error(format("The validation of the specified custom docker registry config with CRN('%s') failed on the Compute API",
                            customDockerRegistryParameters.crn()));
                }
            }
            if (dataServices.azure() != null) {
                resultBuilder.merge(
                        managedIdentityRoleValidator.validateEncryptionRole(dataServices.azure().sharedManagedIdentity()));
            }
        }
        return resultBuilder.build();
    }
}
