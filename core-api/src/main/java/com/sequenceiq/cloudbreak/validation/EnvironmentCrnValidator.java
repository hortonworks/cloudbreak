package com.sequenceiq.cloudbreak.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.common.api.util.ValidatorUtil;

public class EnvironmentCrnValidator implements ConstraintValidator<ValidEnvironmentCrn, String> {

    @Override
    public boolean isValid(String req, ConstraintValidatorContext constraintValidatorContext) {
        constraintValidatorContext.disableDefaultConstraintViolation();

        if (Strings.isNullOrEmpty(req)) {
            ValidatorUtil.addConstraintViolation(constraintValidatorContext, "Environment CRN cannot be null or empty.");
            return false;
        } else if (!Crn.isCrn(req)) {
            ValidatorUtil.addConstraintViolation(constraintValidatorContext, "Invalid crn provided");
            return false;
        }
        return true;
    }

}
