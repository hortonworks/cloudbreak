package com.sequenceiq.cloudbreak.controller.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Constraint(validatedBy = VolumeCountValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidVolume {

    String message() default "Invalid volume count or size";

    int maxCount();

    int minCount();

    int maxSize();

    int minSize();

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
