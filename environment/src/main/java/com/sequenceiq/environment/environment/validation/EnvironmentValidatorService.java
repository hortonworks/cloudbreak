package com.sequenceiq.environment.environment.validation;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAttachRequest;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAwareResource;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentAttachValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentCreationValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentDetachValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentRegionValidator;

@Service
public class EnvironmentValidatorService {

    private final EnvironmentCreationValidator creationValidator;

    private final EnvironmentAttachValidator attachValidator;

    private final EnvironmentDetachValidator detachValidator;

    private final EnvironmentRegionValidator regionValidator;

    public EnvironmentValidatorService(EnvironmentCreationValidator creationValidator, EnvironmentAttachValidator attachValidator,
            EnvironmentDetachValidator detachValidator, EnvironmentRegionValidator regionValidator) {
        this.creationValidator = creationValidator;
        this.attachValidator = attachValidator;
        this.detachValidator = detachValidator;
        this.regionValidator = regionValidator;
    }

    public ValidationResult validateCreation(Environment environment, EnvironmentCreationDto request, CloudRegions cloudRegions) {
        return creationValidator.validate(environment, request, cloudRegions);
    }

    public ValidationResult validateProxyAttach(EnvironmentAttachRequest request) {
        return attachValidator.validate(request);
    }

    public ValidationResult validateDetachForEnvironment(Environment environment, Map<? extends EnvironmentAwareResource, Set<String>> resourcesToClusters) {
        return detachValidator.validateForEnvironment(environment, resourcesToClusters);
    }

    public ValidationResult validateDetachForResource(EnvironmentAwareResource resource, Map<EnvironmentView, Set<String>> envsToClusters) {
        return detachValidator.validateForResource(resource, envsToClusters);
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
