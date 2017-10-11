package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJsonV2;
import com.sequenceiq.cloudbreak.api.model.UpdateStackRequestV2;

public class UpdateStackRequestV2Validator implements ConstraintValidator<ValidUpdateStackRequestV2, UpdateStackRequestV2> {

    @Override
    public void initialize(ValidUpdateStackRequestV2 constraintAnnotation) {
    }

    @Override
    public boolean isValid(UpdateStackRequestV2 value, ConstraintValidatorContext context) {
        int updateResources = 0;
        if (value.getStatus() != null) {
            updateResources++;
        }
        InstanceGroupAdjustmentJsonV2 instanceGroupAdjustment = value.getInstanceGroupAdjustment();
        if (instanceGroupAdjustment != null) {
            updateResources++;
            if (instanceGroupAdjustment.getDesiredCount() < 0) {
                addConstraintViolation(context, "Invalid PUT request on this resource. DesiredNodeCount has to >= 0.");
            }
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