package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.apache.commons.lang3.ArrayUtils;

import com.sequenceiq.cloudbreak.validation.Choice.ChoiceValidator;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ChoiceValidator.class)
public @interface Choice {

    int[] intValues();

    String message() default "Value must match one item in allowed set.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ChoiceValidator implements ConstraintValidator<Choice, Object> {

        private int[] intValues;

        @Override
        public void initialize(Choice constraintAnnotation) {
            intValues = constraintAnnotation.intValues();
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            if (!(value instanceof Integer)) {
                return false;
            }
            return ArrayUtils.contains(intValues, (int) value);
        }

    }
}
