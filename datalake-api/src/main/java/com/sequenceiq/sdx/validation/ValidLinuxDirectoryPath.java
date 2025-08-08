package com.sequenceiq.sdx.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = LinuxDirectoryPathValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLinuxDirectoryPath {

    String message() default "Invalid Linux directory path";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}