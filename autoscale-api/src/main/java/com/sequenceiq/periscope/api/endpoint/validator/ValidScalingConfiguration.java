package com.sequenceiq.periscope.api.endpoint.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = ScalingConfigurationRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidScalingConfiguration {

    String message() default "ScalingConfiguration contains invalid configuration";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
