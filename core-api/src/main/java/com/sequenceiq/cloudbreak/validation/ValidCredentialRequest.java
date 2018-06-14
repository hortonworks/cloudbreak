package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Constraint(validatedBy = CredentialRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCredentialRequest {
    String message() default "Credential request contains invalid data";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
