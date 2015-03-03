package com.sequenceiq.cloudbreak.controller.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.controller.json.UpdateStackJson;

public class UpdateStackRequestValidator implements ConstraintValidator<ValidUpdateStackRequest, UpdateStackJson> {

    @Override
    public void initialize(ValidUpdateStackRequest constraintAnnotation) {
        return;
    }

    @Override
    public boolean isValid(UpdateStackJson value, ConstraintValidatorContext context) {
        int updateResources = 0;
        if (value.getStatus() != null) {
            updateResources++;
        }
        if (value.getInstanceGroupAdjustment() != null) {
            updateResources++;
        }
        if (value.getAllowedSubnets() != null) {
            updateResources++;
        }
        if (updateResources != 1) {
            addConstraintViolation(context, "Invalid PUT request on this resource. 1 update request is allowed at a time.");
            return false;
        }
        return true;
    }

    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("status")
                .addConstraintViolation();
    }

}