package com.sequenceiq.cloudbreak.type;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;

public enum KerberosType {
    CB_MANAGED("*tcpAllowed", "*masterKey", "*admin", "*password"),
    EXISTING_AD("*tcpAllowed", "*principal", "*password", "*url", "adminUrl", "*realm", "*ldapUrl", "*containerDn"),
    EXISTING_MIT("*tcpAllowed", "*principal", "*password", "*url", "adminUrl", "*realm"),
    CUSTOM("*tcpAllowed", "*principal", "*password", "*descriptor", "krb5Conf");

    private final Map<String, Boolean> fields;

    KerberosType(String... fieldsNames) {
        fields = Stream.of(fieldsNames).collect(Collectors.toMap(
                n -> n.replace("*", ""),
                n -> n.indexOf('*') == 0));
    }

    public static KerberosType valueOf(KerberosRequest request) {
        Class<?> clazz = request.getClass();
        Map<Field, Method> declaredPairs = collectFieldMethodPairs(clazz.getDeclaredFields(), clazz.getDeclaredMethods());
        declaredPairs.putAll(collectFieldMethodPairs(clazz.getSuperclass().getDeclaredFields(), clazz.getSuperclass().getDeclaredMethods()));
        try {
            for (KerberosType type : KerberosType.values()) {
                boolean match = true;
                for (Entry<Field, Method> pair : declaredPairs.entrySet()) {
                    Object value = pair.getValue().invoke(request);
                    Boolean required = type.fields.get(pair.getKey().getName());
                    match = required == null ? !hasLength(value) : !required || hasLength(value);
                    if (!match) {
                        break;
                    }
                }
                if (match) {
                    return type;
                }
            }
        } catch (InvocationTargetException | IllegalAccessException ignored) {
        }
        return null;
    }

    private static Map<Field, Method> collectFieldMethodPairs(Field[] fields, Method[] methods) {
        return Stream.of(fields).filter(f -> !"id".equals(f.getName()) && !f.getName().startsWith("$")).collect(Collectors.toMap(f -> f, f -> {
            String getter = "get" + StringUtils.capitalize(f.getName());
            return Stream.of(methods).filter(m -> m.getName().equals(getter)).findFirst().get();
        }));
    }

    private static boolean hasLength(Object value) {
        return value != null && StringUtils.hasLength(value.toString());
    }
}
