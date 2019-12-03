package com.sequenceiq.environment.api.v1.environment.validator.cidr;

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
@Constraint(validatedBy = CidrValidator.class)
public @interface ValidCidr {
    String message() default "The format of the CIDR is not accepted.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
