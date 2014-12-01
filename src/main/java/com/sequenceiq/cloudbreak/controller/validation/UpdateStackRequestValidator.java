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
        if (value.getStatus() != null && value.getHostGroupAdjustment() != null) {
            addConstraintViolation(context, "Invalid PUT request on this resource. NodeCount and status cannot be set in the same request.");
            return false;
        } else if (value.getStatus() == null && value.getHostGroupAdjustment() == null) {
            addConstraintViolation(context, "Invalid PUT request on this resource. It should contain an update on the status or the node count.");
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