package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = AmbariRepositoryV4Validator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAmbariRepo {

    String message() default "Ambari version is not valid. Only " + AmbariRepositoryV4Validator.MIN_AMBARI_VERSION + " and later versions are supported";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
