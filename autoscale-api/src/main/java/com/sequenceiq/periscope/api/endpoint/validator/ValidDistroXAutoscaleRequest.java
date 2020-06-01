package com.sequenceiq.periscope.api.endpoint.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = DistroXAutoscaleRequestValidator.class)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDistroXAutoscaleRequest {
    String message() default "DistroXAutoscaleRequest validation error";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
