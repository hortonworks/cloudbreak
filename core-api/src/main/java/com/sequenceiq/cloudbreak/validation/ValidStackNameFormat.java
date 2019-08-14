package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = StackNameFormatValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStackNameFormat {

    String message() default "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
