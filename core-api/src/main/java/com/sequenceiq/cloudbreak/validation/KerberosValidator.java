package com.sequenceiq.cloudbreak.validation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;

public class KerberosValidator implements ConstraintValidator<ValidKerberos, KerberosRequest> {

    @Override
    public void initialize(ValidKerberos constraintAnnotation) {
    }

    @Override
    public boolean isValid(KerberosRequest request, ConstraintValidatorContext context) {
        return KrbType.valueOf(request) != null;
    }

    private enum KrbType {
        CB_MANAGED("*tcpAllowed", "*masterKey", "*admin", "*password"),
        EXISTING("*tcpAllowed", "*principal", "*password", "*url", "adminUrl", "*realm", "ldapUrl", "containerDn"),
        CUSTOM("*tcpAllowed", "*principal", "*password", "*descriptor", "krb5Conf");

        private Map<String, Boolean> fields;

        KrbType(String... fieldsNames) {
            fields = Stream.of(fieldsNames).collect(Collectors.toMap(
                n -> n.replace("*", ""),
                n -> n.indexOf('*') == 0));
        }

        private static KrbType valueOf(KerberosRequest request) {
            Class<KerberosRequest> clazz = KerberosRequest.class;
            try {
                for (KrbType type : KrbType.values()) {
                    boolean match = true;
                    for (Field field : clazz.getDeclaredFields()) {
                        Object value = clazz.getDeclaredMethod("get" + StringUtils.capitalize(field.getName())).invoke(request);
                        Boolean required = type.fields.get(field.getName());
                        match = required == null ? !hasLength(value) : !required || hasLength(value);
                        if (!match) {
                            break;
                        }
                    }
                    if (match) {
                        return type;
                    }
                }
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
            }
            return null;
        }
    }

    private static boolean hasLength(Object value) {
        return value != null && StringUtils.hasLength(value.toString());
    }
}