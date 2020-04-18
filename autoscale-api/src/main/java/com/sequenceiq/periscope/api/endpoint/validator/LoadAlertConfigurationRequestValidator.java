package com.sequenceiq.periscope.api.endpoint.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.validation.ValidatorUtil;
import com.sequenceiq.periscope.api.model.LoadAlertConfigurationRequest;

public class LoadAlertConfigurationRequestValidator implements ConstraintValidator<ValidLoadAlertConfiguration, LoadAlertConfigurationRequest> {

    @Override
    public void initialize(ValidLoadAlertConfiguration constraintAnnotation) {
    }

    @Override
    public boolean isValid(LoadAlertConfigurationRequest request, ConstraintValidatorContext context) {
        boolean validRequest = true;
        if (request.getMinResourceValue() >= request.getMaxResourceValue()) {
            validRequest = false;
            String message = "The specified loadAlertConfiguration is not valid because the minResourceValue " +
                    " must be less than maxResourceValue.";
            ValidatorUtil.addConstraintViolation(context, message, "minResourceValue")
                    .disableDefaultConstraintViolation();
        }
        return validRequest;
    }

}