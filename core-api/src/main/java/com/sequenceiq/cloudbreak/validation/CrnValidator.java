package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

public class CrnValidator implements ConstraintValidator<ValidCrn, String> {

    @Override
    public boolean isValid(String req, ConstraintValidatorContext constraintValidatorContext) {
        constraintValidatorContext.disableDefaultConstraintViolation();

        if (req == null) {
            return true;
        }
        if (!Crn.isCrn(req)) {
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("Invalid crn provided")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

}
