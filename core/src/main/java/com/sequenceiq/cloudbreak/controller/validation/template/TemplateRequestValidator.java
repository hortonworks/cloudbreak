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
    public ValidationResult validate(TemplateRequest subject) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (Objects.isNull(subject)) {
            resultBuilder.error("Template request cannot be null in the instance group request.");
        } else {
            resultBuilder.ifError(() -> subject.getRootVolumeSize() != null && subject.getRootVolumeSize() < 1,
                    "Root volume size cannot be smaller than 1 gigabyte.");
        }
        return resultBuilder.build();
    }
}
