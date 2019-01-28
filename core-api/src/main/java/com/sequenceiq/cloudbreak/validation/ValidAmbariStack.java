package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = StackRepositoryV4Validator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAmbariStack {

    String message() default "HDP version is not valid. Only 2.6 and later versions are supported";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
