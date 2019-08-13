package com.sequenceiq.cloudbreak.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class StackNameFormatValidator implements ConstraintValidator<ValidStackNameFormat, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        Pattern pattern = Pattern.compile("(^[a-z][-a-z0-9]*[a-z0-9]$)");
        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }
}