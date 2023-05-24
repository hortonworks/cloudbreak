package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.common.api.util.ValidatorUtil;

public class DatalakeCrnValidator implements ConstraintValidator<DatalakeCrn, String> {

    @Override
    public boolean isValid(String req, ConstraintValidatorContext constraintValidatorContext) {
        constraintValidatorContext.disableDefaultConstraintViolation();

        if (req == null) {
            return true;
        }
        if (Crn.isCrn(req) && !Crn.fromString(req).getResourceType().equals(Crn.ResourceType.DATALAKE)) {
            String messageTemplate = "Crn is not belong to a datalake cluster";
            ValidatorUtil.addConstraintViolation(constraintValidatorContext, messageTemplate);
            return false;
        } else if (!Crn.isCrn(req)) {
            String messageTemplate = "Crn is not valid";
            ValidatorUtil.addConstraintViolation(constraintValidatorContext, messageTemplate);
            return false;
        }
        return true;
    }

}
