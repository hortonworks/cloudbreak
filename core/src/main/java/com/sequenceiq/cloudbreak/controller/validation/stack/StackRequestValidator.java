package com.sequenceiq.cloudbreak.controller.validation.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;

@Component
public class StackRequestValidator implements Validator<StackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRequestValidator.class);

    private final Validator<TemplateRequest> templateRequestValidator;

    public StackRequestValidator(Validator<TemplateRequest> templateRequestValidator) {
        this.templateRequestValidator = templateRequestValidator;
    }

    @Override
    public ValidationResult validate(StackRequest stackRequest) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (CollectionUtils.isEmpty(stackRequest.getInstanceGroups())) {
            resultBuilder.error("Stack request must contain instance groups.");
        }
        resultBuilder = validateTemplates(stackRequest, resultBuilder);
        return resultBuilder.build();
    }

    private ValidationResultBuilder validateTemplates(StackRequest stackRequest, ValidationResultBuilder resultBuilder) {
        stackRequest.getInstanceGroups()
                .stream()
                .map(i -> templateRequestValidator.validate(i.getTemplate()))
                .reduce(ValidationResult::merge)
                .ifPresent(resultBuilder::merge);
        return resultBuilder;
    }
}

