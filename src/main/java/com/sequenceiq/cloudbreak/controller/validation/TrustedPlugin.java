package com.sequenceiq.cloudbreak.controller.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = TrustedPluginValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface TrustedPlugin {

    String message() default "Only plugins from a trusted Github source are accepted and the repository must follow the naming convention 'consul-plugins-*'.";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
