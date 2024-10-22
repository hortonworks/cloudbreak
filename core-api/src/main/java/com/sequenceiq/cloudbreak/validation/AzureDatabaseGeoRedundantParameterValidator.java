package com.sequenceiq.cloudbreak.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.sequenceiq.common.api.util.ValidatorUtil;

public class AzureDatabaseGeoRedundantParameterValidator implements ConstraintValidator<ValidAzureDatabaseGeoRedundantParameter, Boolean> {

    @Override
    public boolean isValid(Boolean value, ConstraintValidatorContext context) {
        if (value != null && value == Boolean.TRUE) {
            ValidatorUtil.addConstraintViolation(context, "GeoRedundant Azure database is not supported yet.");
            return false;
        }
        return true;
    }
}
