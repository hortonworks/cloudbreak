package com.sequenceiq.common.api.telemetry.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LoggingRequestValidator.class)
public @interface ValidLoggingRequest {

    String message() default "LoggingV4Request contains one or more invalid input field.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
