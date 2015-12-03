package com.sequenceiq.cloudbreak.controller.validation.template;


import javax.validation.Constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.messaging.handler.annotation.Payload;


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TemplateValidator.class)
public @interface ValidTemplate {

    String message() default "Invalid volume configuration!";

    int maxCount();

    int minCount();

    int maxSize();

    int minSize();

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
