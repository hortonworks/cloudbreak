package com.sequenceiq.periscope.api.endpoint.validator;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.common.api.util.ValidatorUtil;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.ScalingPolicyBase;

public class ScalingPolicyRequestValidator implements ConstraintValidator<ValidScalingPolicy, ScalingPolicyBase> {

    @Inject
    private ScalingHardLimitsService scalingHardLimitsService;

    @Override
    public void initialize(ValidScalingPolicy constraintAnnotation) {

    }

    @Override
    public boolean isValid(ScalingPolicyBase value, ConstraintValidatorContext context) {
        if (AdjustmentType.NODE_COUNT.equals(value.getAdjustmentType())
                && scalingHardLimitsService.isViolatingAutoscaleMaxStepInNodeCount(value.getScalingAdjustment())) {
            String message = String.format("Maximum upscale step is %d node(s)", scalingHardLimitsService.getMaxAutoscaleStepInNodeCount());
            ValidatorUtil.addConstraintViolation(context, message, "scalingAdjustment")
                    .disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }
}
