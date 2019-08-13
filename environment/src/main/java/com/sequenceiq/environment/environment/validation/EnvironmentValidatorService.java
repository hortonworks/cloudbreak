package com.sequenceiq.environment.environment.validation;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.validation.cloudstorage.EnvironmentLogStorageLocationValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentCreationValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentRegionValidator;

@Service
public class EnvironmentValidatorService {

    private final EnvironmentCreationValidator creationValidator;

    private final EnvironmentRegionValidator regionValidator;

    private final EnvironmentLogStorageLocationValidator logStorageLocationValidator;

    public EnvironmentValidatorService(EnvironmentCreationValidator creationValidator, EnvironmentRegionValidator regionValidator,
            EnvironmentLogStorageLocationValidator logStorageLocationValidator) {
        this.creationValidator = creationValidator;
        this.regionValidator = regionValidator;
        this.logStorageLocationValidator = logStorageLocationValidator;
    }

    public ValidationResult validateCreation(Environment environment, EnvironmentCreationDto request, CloudRegions cloudRegions) {
        return creationValidator.validate(environment, request, cloudRegions);
    }

    public ValidationResultBuilder validateRegions(Set<String> requestedRegions, CloudRegions cloudRegions,
            String cloudPlatform, ValidationResultBuilder resultBuilder) {
        return regionValidator.validateRegions(requestedRegions, cloudRegions, cloudPlatform, resultBuilder);
    }

    public ValidationResultBuilder validateLocation(LocationDto location, Set<String> requestedRegions,
            Environment environment, ValidationResultBuilder resultBuilder) {
        return regionValidator.validateLocation(location, requestedRegions, environment, resultBuilder);
    }

    public ValidationResult validateTelemetryLoggingStorageLocation(String userCrn, Environment environment) {
        return logStorageLocationValidator.validateTelemetryLoggingStorageLocation(userCrn, environment);
    }
}
