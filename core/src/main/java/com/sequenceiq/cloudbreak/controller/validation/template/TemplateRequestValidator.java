package com.sequenceiq.cloudbreak.controller.validation.template;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;

@Component
public class TemplateRequestValidator implements Validator<TemplateRequest> {

    @Override
    public ValidationResult validate(TemplateRequest request) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (Objects.isNull(request)) {
            resultBuilder.error("Template request cannot be null in the instance group request.");
        }
        return resultBuilder.build();
    }
}
