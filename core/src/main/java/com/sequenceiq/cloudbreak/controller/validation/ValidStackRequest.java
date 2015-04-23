package com.sequenceiq.cloudbreak.controller.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StackJsonValidator.class)
public @interface ValidStackRequest {

    String message() default "Invalid stack request parameters.";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
