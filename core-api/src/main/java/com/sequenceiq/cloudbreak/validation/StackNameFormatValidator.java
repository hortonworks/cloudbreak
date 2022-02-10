package com.sequenceiq.cloudbreak.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import io.netty.util.internal.StringUtil;

public class StackNameFormatValidator implements ConstraintValidator<ValidStackNameFormat, String> {

    private static final Pattern NAME_PATTERN = Pattern.compile("(^[a-z][-a-z0-9]*[a-z0-9]$)");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StringUtil.isNullOrEmpty(value)) {
            Matcher matcher = NAME_PATTERN.matcher(value);
            return matcher.matches();
        }
        return false;
    }
}