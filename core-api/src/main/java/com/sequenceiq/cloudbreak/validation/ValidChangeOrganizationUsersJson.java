package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = ChangeOrganizationUsersJsonValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidChangeOrganizationUsersJson {

    String message() default "The request must be a valid JSON with the required fields";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
