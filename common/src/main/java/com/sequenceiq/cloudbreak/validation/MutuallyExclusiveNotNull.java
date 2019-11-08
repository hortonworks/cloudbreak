package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import org.apache.commons.lang3.ObjectUtils;
import com.sequenceiq.cloudbreak.validation.MutuallyExclusiveNotNull.MutuallyExclusiveNotNullValidator;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MutuallyExclusiveNotNullValidator.class)
public @interface MutuallyExclusiveNotNull {

    String[] fieldGroups();

    String message() default "Only one field should be not null.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class MutuallyExclusiveNotNullValidator implements ConstraintValidator<MutuallyExclusiveNotNull, Object> {

        private String[] fieldGroups;

        @Override
        public void initialize(MutuallyExclusiveNotNull constraintAnnotation) {
            fieldGroups = constraintAnnotation.fieldGroups();
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            if (value == null) {
                return false;
            }
            boolean hasNotNullGroup = false;
            try {
                for (String fieldGroup : fieldGroups) {
                    String[] fieldNames = fieldGroup.split(",");
                    Boolean groupNullity = null;
                    for (String fieldName : fieldNames) {
                        Object fieldValue = getFieldValue(value, fieldName);
                        if (groupNullity == null) {
                            groupNullity = fieldValue == null;
                        } else {
                            if (groupNullity != (fieldValue == null)) {
                                return false;
                            }
                        }
                    }
                    if (!ObjectUtils.defaultIfNull(groupNullity, true)) {
                        if (!hasNotNullGroup) {
                            hasNotNullGroup = true;
                        } else {
                            return false;
                        }
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return hasNotNullGroup;
        }

        private Object getFieldValue(Object value, String fieldName) throws NoSuchFieldException, IllegalAccessException {
            Field field = getInheritedDeclaredField(value.getClass(), fieldName);
            field.setAccessible(true);
            return field.get(value);
        }

        Field getInheritedDeclaredField(Class<?> fromClass, String fieldName) throws NoSuchFieldException {
            Class<?> currentClass = fromClass;
            do {
                Field field;
                try {
                    field = currentClass.getDeclaredField(fieldName);
                    if (field != null) {
                        return field;
                    }
                } catch (NoSuchFieldException | SecurityException ignore) {
                    // Nothing. We'll try to get field from superclass
                }
                currentClass = currentClass.getSuperclass();
            } while (currentClass != null && !currentClass.equals(Object.class));
            // If we got here, we'll throw an exception
            throw new NoSuchFieldException(fieldName);
        }
    }
}