package com.sequenceiq.cloudbreak.validation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;

public class KerberosValidator implements ConstraintValidator<ValidKerberos, KerberosRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosValidator.class);

    @Override
    public void initialize(ValidKerberos constraintAnnotation) {
    }

    @Override
    public boolean isValid(KerberosRequest request, ConstraintValidatorContext context) {
            Class<?> clazz = request.getClass();
            Map<Field, Method> declaredPairs = collectFieldMethodPairs(clazz.getDeclaredFields(), clazz.getDeclaredMethods());
            declaredPairs.putAll(collectFieldMethodPairs(clazz.getSuperclass().getDeclaredFields(), clazz.getSuperclass().getDeclaredMethods()));
            return !declaredPairs.entrySet().stream()
                    .filter(entry -> isEntryInvalid(request, entry))
                    .peek(entry -> LOGGER.error("Field {} is required for Kerberos in case of type {}.",
                            entry.getKey().getName(), request.getType().name()))
                    .findFirst()
                    .isPresent();
    }

    private boolean isEntryInvalid(KerberosRequest request, Map.Entry<Field, Method> fieldMethodEntry) {
        try {
            RequiredKerberosField kerberosFieldAnnotation = fieldMethodEntry.getKey().getAnnotation(RequiredKerberosField.class);
            return (kerberosFieldAnnotation.types().length == 0 || Arrays.asList(kerberosFieldAnnotation.types()).contains(request.getType()))
                    && fieldMethodEntry.getValue().invoke(request) == null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    private static Map<Field, Method> collectFieldMethodPairs(Field[] fields, Method[] methods) {
        return Stream.of(fields)
                .filter(field -> field.getAnnotation(RequiredKerberosField.class) != null)
                .collect(Collectors.toMap(field -> field, field -> {
                    String getter = "get" + StringUtils.capitalize(field.getName());
                    return Stream.of(methods)
                            .filter(method -> method.getName().equals(getter))
                            .findFirst()
                            .get();
        }));
    }
}