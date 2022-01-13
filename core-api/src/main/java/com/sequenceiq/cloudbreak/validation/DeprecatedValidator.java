package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.common.api.util.ValidatorUtil;

public class DeprecatedValidator implements ConstraintValidator<ValidDeprecated, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isEmpty(value)) {
            return true;
        } else {
            ValidatorUtil.addConstraintViolation(context,
                    "You have submitted a field that is not accepted anymore. This is usually due to deprecation or security reasons.");
            return false;
        }
    }
}
