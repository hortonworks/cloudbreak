package com.sequenceiq.environment.api.v1.proxy.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoProxyListValidator.class)
public @interface ValidNoProxyList {
    String message() default "no_proxy can contain comma-separated list of 'host[:port]' and 'IP[:port]' elements";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
