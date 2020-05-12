package com.sequenceiq.sdx.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UpgradeRequestValidator.class)
public @interface ValidUpgradeRequest {

    String message() default "Upgrade request is invalid. "
            + "Either one of 'runtime', 'imageId', 'lockComponents' parameters "
            + "or both 'imageId' and 'lockComponents' parameter must be specified in the request";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}