package com.sequenceiq.cloudbreak.rotation.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import com.sequenceiq.cloudbreak.rotation.validator.MultiSecretTypeValidator;

@Documented
@Constraint(validatedBy = MultiSecretTypeValidator.class)
@Target({ FIELD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMultiSecretType {

    String message() default "Only valid multi cluster secret rotation type is allowed!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
