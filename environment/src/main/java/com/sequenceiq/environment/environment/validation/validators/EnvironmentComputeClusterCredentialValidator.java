package com.sequenceiq.environment.environment.validation.validators;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeService;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterCredentialValidationResponse;

@Service
public class EnvironmentComputeClusterCredentialValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentComputeClusterCredentialValidator.class);

    @Inject
    private ExternalizedComputeService externalizedComputeService;

    public ValidationResult validate(EnvironmentValidationDto environmentValidationDto) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        if (environmentDto.isEnableComputeCluster()) {
            ExternalizedComputeClusterCredentialValidationResponse validateCredentialResponse = externalizedComputeService.validateCredential(
                    environmentDto.getResourceCrn(), environmentDto.getCredential().getName(), environmentDto.getLocation().getName());
            if (!validateCredentialResponse.isSuccessful()) {
                LOGGER.debug("Credential validation failed: {}", validateCredentialResponse);
                for (String validationResult : validateCredentialResponse.getValidationResults()) {
                    resultBuilder.error("Credential validation failed for compute cluster: " + validationResult);
                }
            }
        }
        return resultBuilder.build();
    }
}
