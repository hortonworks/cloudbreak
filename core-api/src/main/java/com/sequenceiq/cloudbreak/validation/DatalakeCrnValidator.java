package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

public class DatalakeCrnValidator implements ConstraintValidator<DatalakeCrn, String> {

    @Override
    public boolean isValid(String req, ConstraintValidatorContext constraintValidatorContext) {
        constraintValidatorContext.disableDefaultConstraintViolation();

        if (req == null) {
            return true;
        }
        if (Crn.isCrn(req) && !Crn.fromString(req).getResourceType().equals(Crn.ResourceType.DATALAKE)) {
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("Crn is not belong to a datalake cluster")
                    .addConstraintViolation();
            return false;
        } else if (!Crn.isCrn(req)) {
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("Crn is not valid")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

}
