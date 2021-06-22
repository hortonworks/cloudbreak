package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

public class EnvironmentCrnValidator implements ConstraintValidator<ValidEnvironmentCrn, String> {

    @Override
    public boolean isValid(String req, ConstraintValidatorContext constraintValidatorContext) {
        constraintValidatorContext.disableDefaultConstraintViolation();

        if (Strings.isNullOrEmpty(req)) {
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("Environment CRN cannot be null or empty.")
                    .addConstraintViolation();
            return false;
        } else if (!Crn.isCrn(req)) {
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("Invalid crn provided")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

}
