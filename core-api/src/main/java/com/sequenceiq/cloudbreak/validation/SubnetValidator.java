package com.sequenceiq.cloudbreak.validation;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SubnetValidator implements ConstraintValidator<ValidSubnet, String> {

    private static final Pattern PATTERN = Pattern.compile(
            "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/([0-9]|[1-2][0-9]|3[0-2]))$");

    @Override
    public void initialize(ValidSubnet constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.indexOf("0.") != 0 && PATTERN.matcher(value).find();
    }
}