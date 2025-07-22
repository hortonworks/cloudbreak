package com.sequenceiq.cloudbreak.util;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.sequenceiq.common.api.util.ValidatorUtil;

public class OneOfEnumValidator implements ConstraintValidator<OneOfEnum, CharSequence> {

    private Set<String> acceptedValues;

    private String message;

    private String fieldName;

    @Override
    public void initialize(OneOfEnum annotation) {
        this.message = annotation.message();
        this.fieldName = annotation.fieldName();
        acceptedValues = Stream.of(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        } else {
            boolean valid = acceptedValues.contains(value.toString().toUpperCase(Locale.ROOT));
            if (!valid) {
                getError(context);
            }
            return valid;
        }
    }

    private void getError(ConstraintValidatorContext context) {
        ValidatorUtil.addConstraintViolation(context, String.format(message, acceptedValues), fieldName)
                .disableDefaultConstraintViolation();
    }
}
