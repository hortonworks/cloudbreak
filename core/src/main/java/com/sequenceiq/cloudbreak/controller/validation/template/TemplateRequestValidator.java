package com.sequenceiq.cloudbreak.controller.validation.template;

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
        resultBuilder.ifError(() -> request.getRootVolumeSize() != null && request.getRootVolumeSize() < 1,
                "Root volume size cannot be smaller than 1 gigabyte.");
        return resultBuilder.build();
    }
}
