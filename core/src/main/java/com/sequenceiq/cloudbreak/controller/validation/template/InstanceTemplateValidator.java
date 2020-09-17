package com.sequenceiq.cloudbreak.controller.validation.template;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.validation.Validator;

@Component
public class InstanceTemplateValidator implements Validator<Template> {

    @Override
    public ValidationResult validate(Template subject) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (Objects.isNull(subject)) {
            resultBuilder.error("Template request cannot be null in the instance group request.");
        } else {
            resultBuilder.ifError(() -> subject.getRootVolumeSize() != null
                            && subject.getRootVolumeSize() < 1,
                    "Root volume size cannot be smaller than 1 gigabyte.");
        }
        return resultBuilder.build();
    }
}
