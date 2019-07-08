package com.sequenceiq.environment.environment.validation;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentCreationValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentRegionValidator;

@Service
public class EnvironmentValidatorService {

    private final EnvironmentCreationValidator creationValidator;

    private final EnvironmentRegionValidator regionValidator;

    public EnvironmentValidatorService(EnvironmentCreationValidator creationValidator, EnvironmentRegionValidator regionValidator) {
        this.creationValidator = creationValidator;
        this.regionValidator = regionValidator;
    }

    public ValidationResult validateCreation(Environment environment, EnvironmentCreationDto request, CloudRegions cloudRegions) {
        return creationValidator.validate(environment, request, cloudRegions);
    }

    public ValidationResult.ValidationResultBuilder validateRegions(Set<String> requestedRegions, CloudRegions cloudRegions,
            String cloudPlatform, ValidationResult.ValidationResultBuilder resultBuilder) {
        return regionValidator.validateRegions(requestedRegions, cloudRegions, cloudPlatform, resultBuilder);
    }

    public ValidationResult.ValidationResultBuilder validateLocation(LocationDto location, Set<String> requestedRegions,
            Environment environment, ValidationResult.ValidationResultBuilder resultBuilder) {
        return regionValidator.validateLocation(location, requestedRegions, environment, resultBuilder);
    }
}
