package com.sequenceiq.periscope.api.endpoint.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.common.api.util.ValidatorUtil;
import com.sequenceiq.periscope.api.model.ScalingConfigurationRequest;

public class ScalingConfigurationRequestValidator implements ConstraintValidator<ValidScalingConfiguration, ScalingConfigurationRequest> {

    @Override
    public void initialize(ValidScalingConfiguration constraintAnnotation) {
    }

    @Override
    public boolean isValid(ScalingConfigurationRequest request, ConstraintValidatorContext context) {
        boolean validRequest = true;
        if (request.getMaxSize() < request.getMinSize()) {
            validRequest = false;
            String message = "The specified ScalingConfiguration is not valid because the minimum cluster size can not be more than"
                    + " maximum cluster size.";
            ValidatorUtil.addConstraintViolation(context, message, "minSize")
                    .disableDefaultConstraintViolation();
        } else if (request.getMaxSize() < 0) {
            validRequest = false;
            String message = "The specified ScalingConfiguration is not valid because the maximum size smaller than zero.";
            ValidatorUtil.addConstraintViolation(context, message, "maxSize")
                    .disableDefaultConstraintViolation();
        } else if (request.getMinSize() < 0) {
            validRequest = false;
            String message = "The specified ScalingConfiguration is not valid because the minimum size smaller than zero.";
            ValidatorUtil.addConstraintViolation(context, message, "minSize")
                    .disableDefaultConstraintViolation();
        }
        return validRequest;
    }

}