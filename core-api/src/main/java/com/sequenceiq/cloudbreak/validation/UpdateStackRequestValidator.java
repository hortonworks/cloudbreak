package com.sequenceiq.cloudbreak.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.common.api.util.ValidatorUtil;

public class UpdateStackRequestValidator implements ConstraintValidator<ValidUpdateStackRequest, UpdateStackV4Request> {

    @Override
    public void initialize(ValidUpdateStackRequest constraintAnnotation) {
    }

    @Override
    public boolean isValid(UpdateStackV4Request value, ConstraintValidatorContext context) {
        int updateResources = 0;
        if (value.getStatus() != null) {
            updateResources++;
        }
        InstanceGroupAdjustmentV4Request instanceGroupAdjustment = value.getInstanceGroupAdjustment();
        if (instanceGroupAdjustment != null) {
            updateResources++;
            if (instanceGroupAdjustment.getScalingAdjustment() == null) {
                ValidatorUtil.addConstraintViolation(context,
                        "Invalid PUT request on this resource. Scaling adjustment has to be specified for Instance Group adjustment", "status");
                return false;
            }
            if (value.getWithClusterEvent() && instanceGroupAdjustment.getScalingAdjustment() < 0) {
                ValidatorUtil.addConstraintViolation(context,
                        "Invalid PUT request on this resource. Update event has to be upscale if you define withClusterEvent = 'true'.", "status");
                return false;
            }
        }

        if (updateResources != 1) {
            ValidatorUtil.addConstraintViolation(context, "Invalid PUT request on this resource. 1 update request is allowed at a time.", "status");
            return false;
        }
        return true;
    }

}
