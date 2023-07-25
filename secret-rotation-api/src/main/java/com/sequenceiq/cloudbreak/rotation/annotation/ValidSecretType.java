package com.sequenceiq.cloudbreak.rotation.annotation;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.validator.SecretTypeValidator;

@Documented
@Constraint(validatedBy = SecretTypeValidator.class)
@Target({ FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSecretType {

    Class<? extends SecretType>[] allowedTypes();

    boolean internalOnlyAllowed() default false;

    String message() default "Only valid secret rotation type is allowed!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
