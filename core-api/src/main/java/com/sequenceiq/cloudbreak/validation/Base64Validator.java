package com.sequenceiq.cloudbreak.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.codec.binary.Base64;
import org.springframework.util.StringUtils;

public class Base64Validator implements ConstraintValidator<ValidBase64, String> {

    @Override
    public void initialize(ValidBase64 constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return !StringUtils.hasLength(value) || Base64.isBase64(value);
    }
}
