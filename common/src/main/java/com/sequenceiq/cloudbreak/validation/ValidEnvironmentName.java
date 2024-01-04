package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidEnvironmentNameValidator.class)
public @interface ValidEnvironmentName {

    String message() default "The environments's name must contain lowercase alphanumeric characters and hyphens " +
            "and must start with an alphanumeric character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


}
