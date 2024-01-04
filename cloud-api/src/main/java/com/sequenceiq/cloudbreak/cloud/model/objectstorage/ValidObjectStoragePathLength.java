package com.sequenceiq.cloudbreak.cloud.model.objectstorage;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = ObjectStoragePathLengthValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidObjectStoragePathLength {

    String message() default "The length of the name has to be in range of 1 to 99999";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
