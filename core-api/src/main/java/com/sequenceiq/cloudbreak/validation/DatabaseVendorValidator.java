package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.common.api.util.ValidatorUtil;

public class DatabaseVendorValidator implements ConstraintValidator<ValidDatabaseVendor, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (value == null) {
            return true;
        }
        if (value.isEmpty()) {
            ValidatorUtil.addConstraintViolation(context, "Database vendor may not be empty");
            return false;
        }

        try {
            DatabaseVendor.fromValue(value);
            return true;
        } catch (UnsupportedOperationException e) {
            ValidatorUtil.addConstraintViolation(context, value + " is not a valid database vendor");
            return false;
        }
    }
}
