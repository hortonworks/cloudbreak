package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.apache.commons.lang3.ObjectUtils;

import com.sequenceiq.cloudbreak.validation.MutuallyExclusiveNotNull.MutuallyExclusiveNotNullValidator;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidEnvironmentNameValidator.class)
public @interface ValidEnvironmentName {

    String message() default "The environments's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


}