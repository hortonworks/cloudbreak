package com.sequenceiq.cloudbreak.rotation.annotation;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.validator.MultiSecretTypeValidator;

@Documented
@Constraint(validatedBy = MultiSecretTypeValidator.class)
@Target({ FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMultiSecretType {

    Class<? extends MultiSecretType>[] allowedTypes();

    String message() default "Only valid multi cluster secret rotation type is allowed!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
