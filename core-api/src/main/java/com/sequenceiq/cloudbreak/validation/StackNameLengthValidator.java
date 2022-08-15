package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.Range;

public class StackNameLengthValidator implements ConstraintValidator<ValidStackNameLength, String> {

    private static final Integer MIN_LENGTH = 5;

    private static final Integer MAX_LENGTH = 40;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && Range.between(MIN_LENGTH, MAX_LENGTH).contains(value.length());
    }
}