package com.sequenceiq.environment.api.v1.environment.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OutboundInternetTrafficValidator.class)
public @interface ValidOutboundInternetTrafficNetworkRequest {
    String message() default "If OutboundInternetTraffic is disabled ServiceEndpointCreation must be enabled";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
