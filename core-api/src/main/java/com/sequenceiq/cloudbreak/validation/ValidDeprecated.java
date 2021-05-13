package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DeprecatedValidator.class)
public @interface ValidDeprecated {

    String message() default "This field has been deprecated and is not in use anymore.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
