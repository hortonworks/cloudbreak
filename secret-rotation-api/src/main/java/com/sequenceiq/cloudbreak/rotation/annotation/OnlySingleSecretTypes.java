package com.sequenceiq.cloudbreak.rotation.annotation;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.sequenceiq.cloudbreak.rotation.validator.SingleSecretTypesValidator;

@Documented
@Constraint(validatedBy = SingleSecretTypesValidator.class)
@Target({ FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface OnlySingleSecretTypes {

    String message() default "Only single secret rotation types allowed!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
