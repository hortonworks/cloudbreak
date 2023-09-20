package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class OneOfEnumValidator implements ConstraintValidator<OneOfEnum, CharSequence> {
    private Set<String> acceptedValues;

    @Override
    public void initialize(OneOfEnum annotation) {
        acceptedValues = Stream.of(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return acceptedValues.contains(value.toString().toUpperCase(Locale.ROOT));
    }
}
