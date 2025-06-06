package com.sequenceiq.cloudbreak.auth.security.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Payload;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AccountId {

    String message() default "In case of internal actor API call you need to specify account id.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
