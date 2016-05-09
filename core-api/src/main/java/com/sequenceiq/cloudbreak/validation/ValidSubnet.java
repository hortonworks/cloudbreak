package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = SubnetValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSubnet {

    String message() default "The subnet field should contain a valid CIDR definition.";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
