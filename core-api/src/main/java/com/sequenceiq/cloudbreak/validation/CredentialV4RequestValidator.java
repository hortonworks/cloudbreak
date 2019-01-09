package com.sequenceiq.cloudbreak.validation;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.CredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;

public class CredentialV4RequestValidator implements ConstraintValidator<ValidCredentialV4BaseRequest, CredentialV4Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialV4RequestValidator.class);

    @Override
    public boolean isValid(CredentialV4Request req, ConstraintValidatorContext constraintValidatorContext) {
        boolean valid = isOnlyOneParameterFieldFilled(req);
        if (!valid) {
            ValidatorUtil.addConstraintViolation(constraintValidatorContext, "Only one parameter instance should be filled!", "status");
        }
        return valid;
    }

    private static boolean isOnlyOneParameterFieldFilled(CredentialV4Request request) {
        Stream<Field> fields = Arrays.stream(request.getClass().getDeclaredFields());
        int notEmptyParamTypeQuantity = 0;
        for (Field field : fields.filter(field -> getInterfacesOfField(field).contains(CredentialV4Parameters.class)).collect(Collectors.toList())) {
            try {
                if (FieldUtils.readField(request, field.getName(), true) != null) {
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
        return Arrays.asList(field.getType().getInterfaces());
    }

}
