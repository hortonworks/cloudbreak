package com.sequenceiq.periscope.api.endpoint.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.periscope.api.model.ScalingPolicyBase;

public class ScalingPolicyRequestValidator implements ConstraintValidator<ValidScalingPolicy, ScalingPolicyBase> {

    @Override
    public void initialize(ValidScalingPolicy constraintAnnotation) {

    }

    @Override
    public boolean isValid(ScalingPolicyBase value, ConstraintValidatorContext context) {
        return true;
    }
}
