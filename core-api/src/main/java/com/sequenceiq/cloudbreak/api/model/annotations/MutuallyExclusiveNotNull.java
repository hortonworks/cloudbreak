package com.sequenceiq.cloudbreak.api.model.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import com.sequenceiq.cloudbreak.api.model.annotations.MutuallyExclusiveNotNull.MutuallyExclusiveNotNullValidator;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MutuallyExclusiveNotNullValidator.class)
public @interface MutuallyExclusiveNotNull {

    String[] fieldNames();

    String message() default "Only one field should be not null.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    @IgnorePojoValidation
    class MutuallyExclusiveNotNullValidator implements ConstraintValidator<MutuallyExclusiveNotNull, Object> {
        private String[] fieldNames;

        @Override
        public void initialize(MutuallyExclusiveNotNull constraintAnnotation) {
            fieldNames = constraintAnnotation.fieldNames();
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            if (value == null) {
                return false;
            }
            boolean hasNotNull = false;
            try {
                for (String fieldName : fieldNames) {
                    Field field = value.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object fieldValue = field.get(value);
                    if (fieldValue != null) {
                        if (!hasNotNull) {
                            hasNotNull = true;
                        } else {
                            return false;
                        }
                    }

                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            return hasNotNull;
        }
    }
}
