package com.sequenceiq.maintenance.api.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

public final class ValidationTestUtil {

    private static final Validator VALIDATOR = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    private ValidationTestUtil() {
    }

    public static Validator validator() {
        return VALIDATOR;
    }
}
