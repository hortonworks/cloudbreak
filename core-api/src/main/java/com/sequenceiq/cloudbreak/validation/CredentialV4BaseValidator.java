package com.sequenceiq.cloudbreak.validation;

import static java.util.Arrays.stream;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.CredentialV4Base;

public class CredentialV4BaseValidator implements ConstraintValidator<ValidCredentialV4Base, CredentialV4Base> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialV4BaseValidator.class);

    @Override
    public boolean isValid(CredentialV4Base base, ConstraintValidatorContext constraintValidatorContext) {
        boolean valid = isOnlyOneParameterFieldFilled(base);
        if (!valid) {
            ValidatorUtil.addConstraintViolation(constraintValidatorContext, "Only one parameter instance should be filled!", "status");
        }
        return valid;
    }

    private static boolean isOnlyOneParameterFieldFilled(CredentialV4Base base) {
        Stream<Field> fields = Stream.concat(stream(base.getClass().getDeclaredFields()), stream(base.getClass().getSuperclass().getDeclaredFields()));
        int notEmptyParamTypeQuantity = 0;
        for (Field field : fields.filter(field -> getInterfacesOfField(field).contains(Mappable.class)).collect(Collectors.toList())) {
            try {
                if (FieldUtils.readField(base, field.getName(), true) != null) {
                    notEmptyParamTypeQuantity++;
                }
            } catch (IllegalAccessException e) {
                LOGGER.warn(String.format("Unable to access field! [%s.%s]", field.getDeclaringClass().getSimpleName(), field.getName()), e);
                return false;
            }
        }
        return notEmptyParamTypeQuantity == 1;
    }

    private static List<Class<?>> getInterfacesOfField(Field field) {
        // assuming that the given field's base class is extending the MappableBase class which implements the Mappable interface
        // if this structure changes, this listing still works, but the verification shall fail at the end
        return Arrays.asList(field.getType().getSuperclass().getInterfaces());
    }

}
