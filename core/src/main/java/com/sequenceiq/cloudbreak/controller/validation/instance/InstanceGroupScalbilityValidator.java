package com.sequenceiq.cloudbreak.controller.validation.instance;

import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.validation.Validator;

import org.springframework.stereotype.Component;

@Component
public class InstanceGroupScalbilityValidator implements Validator<InstanceGroup> {

    @Override
    public ValidationResult validate(InstanceGroup instanceGroup) {
        ValidationResultBuilder builder = new ValidationResultBuilder();
        if (instanceGroup == null) {
            builder.error("Instance group '%s' not found in stack '%s'.");
        } else if (!instanceGroup.getScalingMode().isScalable()) {
            builder.error("Instance group '%s' in stack '%s' is not enabled to scale.");
        }
        return builder.build();
    }
}
