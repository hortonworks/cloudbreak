package com.sequenceiq.cloudbreak.controller.validation.template;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;

@Component
public class InstanceTemplateV4RequestValidator implements Validator<InstanceTemplateV4Request> {

    @Override
    public ValidationResult validate(InstanceTemplateV4Request subject) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (Objects.isNull(subject)) {
            resultBuilder.error("Template request cannot be null in the instance group request.");
        } else {
            resultBuilder.ifError(() -> subject.getRootVolume() != null
                            && subject.getRootVolume().getSize() != null
                            && subject.getRootVolume().getSize() < 1,
                    "Root volume size cannot be smaller than 1 gigabyte.");
        }
        return resultBuilder.build();
    }
}
