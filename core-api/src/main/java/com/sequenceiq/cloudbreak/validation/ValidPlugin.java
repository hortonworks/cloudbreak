package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = PluginValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPlugin {

    String message() default "Only plugins from http, https, git and consul protocols are allowed and base64 encoded plugins with base64:// prefix.";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
