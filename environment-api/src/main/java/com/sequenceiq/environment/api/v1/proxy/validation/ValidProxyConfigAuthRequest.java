package com.sequenceiq.environment.api.v1.proxy.validation;

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
@Constraint(validatedBy = ProxyConfigAuthValidator.class)
public @interface ValidProxyConfigAuthRequest {
    String message() default "If proxy authentication is needed both username and password have to be provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
