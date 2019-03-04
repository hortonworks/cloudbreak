package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GcsCloudStorageParametersValidator.class)
public @interface ValidGcsCloudStorageParameters {

    String message() default "GCS Cloud Storage Parameters contains one or more invalid data.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
