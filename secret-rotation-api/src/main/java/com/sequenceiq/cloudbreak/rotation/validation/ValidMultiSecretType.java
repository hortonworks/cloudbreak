package com.sequenceiq.cloudbreak.rotation.validation;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = MultiSecretTypeValidator.class)
@Target({ FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMultiSecretType {

    String message() default "Not a multi secret rotation!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
