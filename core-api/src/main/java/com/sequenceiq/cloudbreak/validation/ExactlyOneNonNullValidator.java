package com.sequenceiq.cloudbreak.validation;

import java.lang.reflect.Field;
import java.util.Arrays;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.common.api.util.ValidatorUtil;

public class ExactlyOneNonNullValidator implements ConstraintValidator<ValidIfExactlyOneNonNull, Object> {

    private String[] fields;

    @Override
    public void initialize(ValidIfExactlyOneNonNull constraint) {
        fields = constraint.fields();
        if (fields.length == 0) {
            throw new IllegalStateException("This annotation requires at least one field to check");
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (value == null) {
            return true;
        }
        Class<?> clazz = value.getClass();

        int numNonNull = 0;
        for (String fieldName : fields) {
            try {
                Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                if (f.get(value) != null) {
                    numNonNull++;
                }
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("Field " + fieldName + " listed in annotation is not present in class " + clazz);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to access field " + fieldName + " after setting to accessible");
            }
        }

        switch (numNonNull) {
            case 0:
                ValidatorUtil.addConstraintViolation(context, "All fields " + Arrays.toString(fields) + " are null, but one must be non-null");
                return false;
            case 1:
                return true;
            default:
                ValidatorUtil.addConstraintViolation(context, "Two or more fields among " + Arrays.toString(fields) + " are non-null, but only one may be");
                return false;
        }
    }

}
