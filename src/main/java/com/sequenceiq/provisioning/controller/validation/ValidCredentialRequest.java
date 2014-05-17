package com.sequenceiq.provisioning.controller.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CredentialParametersValidator.class)
public @interface ValidCredentialRequest {

    String message() default "Failed to validate credential request parameters.";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
