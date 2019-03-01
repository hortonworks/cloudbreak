package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HttpContentSizeValidator.class)
public @interface ValidHttpContentSize {

    String message() default "The content of the given URL must be less than " + HttpContentSizeValidator.MAX_SIZE + " Mb";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
