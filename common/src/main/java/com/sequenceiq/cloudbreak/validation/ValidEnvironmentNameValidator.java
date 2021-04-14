package com.sequenceiq.cloudbreak.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

class ValidEnvironmentNameValidator implements ConstraintValidator<ValidEnvironmentName, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String patternString = "^(?!.*--)[a-z][-a-z0-9]*[a-z0-9]$";
        Pattern pattern = Pattern.compile(patternString);

        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }
}