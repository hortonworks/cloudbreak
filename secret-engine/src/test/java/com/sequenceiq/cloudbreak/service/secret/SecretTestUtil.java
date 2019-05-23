package com.sequenceiq.cloudbreak.service.secret;

import java.lang.reflect.Field;

import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.service.secret.domain.Secret;

public class SecretTestUtil {
    private SecretTestUtil() {
    }

    public static void setSecretField(Class<?> clazz, String fieldName, Object target, String raw, String secret) {
        Field field = ReflectionUtils.findField(clazz, fieldName);
        field.setAccessible(true);
        try {
            field.set(target, new Secret(raw, secret));
        } catch (IllegalAccessException ignore) {
        }
    }
}
