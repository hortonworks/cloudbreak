package com.sequenceiq.cloudbreak.service.stack.flow;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.Encrypted;

public class ReflectionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionUtils.class);
    private static PBEStringCleanablePasswordEncryptor pbeStringCleanablePasswordEncryptor;

    private ReflectionUtils() {
    }

    public static Map<String, Object> getDeclaredFields(Object object) {
        Map<String, Object> dynamicFields = new HashMap<>();
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            String name = field.getName();
            try {
                dynamicFields.put(name, getValue(field, object));
            } catch (IllegalAccessException e) {
                LOGGER.error("Cannot retrieve field {} from class {}", name, object.getClass().getName());
            }
        }
        return dynamicFields;
    }

    public static void setEncryptor(PBEStringCleanablePasswordEncryptor encryptor) {
        pbeStringCleanablePasswordEncryptor = encryptor;
    }

    private static Object getValue(Field field, Object object) throws IllegalAccessException {
        Object value = field.get(object);
        Encrypted annotation = field.getAnnotation(Encrypted.class);
        return annotation == null ? value : pbeStringCleanablePasswordEncryptor.decrypt((String) value);
    }

}
