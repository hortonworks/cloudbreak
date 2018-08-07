package com.sequenceiq.cloudbreak.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = S3CloudStorageParametersValidator.class)
public @interface ValidS3CloudStorageParameters {

    String message() default "S3 Cloud Storage Parameters contains one or more invalid data.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
