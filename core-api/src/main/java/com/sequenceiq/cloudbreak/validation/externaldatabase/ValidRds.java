package com.sequenceiq.cloudbreak.validation.externaldatabase;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = RdsRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRds {

    String message() default "RdsRequest contains unsupported database and service combination";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
