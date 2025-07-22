package com.sequenceiq.cloudbreak.util;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = OneOfEnumValidator.class)
public @interface OneOfEnum {

    Class<? extends Enum<?>> enumClass();

    String message() default "Must be any of {enumClass}";

    String fieldName() default "fieldName";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
