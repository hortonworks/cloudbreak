package com.sequenceiq.cloudbreak.validation;

import static org.apache.commons.validator.routines.UrlValidator.ALLOW_ALL_SCHEMES;
import static org.apache.commons.validator.routines.UrlValidator.ALLOW_LOCAL_URLS;
import static org.apache.commons.validator.routines.UrlValidator.NO_FRAGMENTS;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.common.api.util.ValidatorUtil;

public class UrlValidator implements ConstraintValidator<ValidUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (value == null) {
            return true;
        }
        if (value.isEmpty()) {
            ValidatorUtil.addConstraintViolation(context, "URL may not be empty");
            return false;
        }

        long validatorOptions = ALLOW_ALL_SCHEMES + ALLOW_LOCAL_URLS + NO_FRAGMENTS;
        org.apache.commons.validator.routines.UrlValidator commonsValidator = new org.apache.commons.validator.routines.UrlValidator(validatorOptions);
        if (!commonsValidator.isValid(value)) {
            ValidatorUtil.addConstraintViolation(context, value + " is not a valid URL");
            return false;
        }
        return true;
    }

}
