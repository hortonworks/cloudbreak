package com.sequenceiq.environment.environment.validation.validators;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAttachRequest;

@Component
public class EnvironmentAttachValidator {

    public ValidationResult validate(EnvironmentAttachRequest request) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        return resultBuilder.build();
    }
}
