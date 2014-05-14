package com.sequenceiq.provisioning.controller.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProvisionParametersValidator.class)
public @interface ValidProvisionRequest {

    String message() default "Failed to validate provision request parameters.";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
