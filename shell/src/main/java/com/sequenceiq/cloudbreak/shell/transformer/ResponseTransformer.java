package com.sequenceiq.cloudbreak.shell.transformer;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ResponseTransformer {

    public Map<String, String> transformToMap(Collection<?> responses, String keyName, String valueName) {
        Map<String, String> transformed = new HashMap<>();

        for (Object object : responses) {
            String key = "";
            String value = "";
            Class<?> current = object.getClass();
            while (current.getSuperclass() != null) {

                for (Field field : current.getDeclaredFields()) {
                    field.setAccessible(true);
                    try {
                        Object o = field.get(object);
                        if (o != null) {
                            if (field.getName().equals(keyName)) {
                                key = o.toString();
                            } else if (field.getName().equals(valueName)) {
                                value = o.toString();
                            }
                        }
                    } catch (IllegalAccessException e) {
                        value = "undefined";
                    }
                }
                current = current.getSuperclass();
            }
            transformed.put(key, value);
        }
        return transformed;
    }

    public Map<String, String> transformObjectToStringMap(Object o, String... exclude) {
        Map<String, String> result = new HashMap<>();
        Class<?> current = o.getClass();
        while (current.getSuperclass() != null) {
            for (Field field : current.getDeclaredFields()) {
                if (!Arrays.asList(exclude).contains(field.getName())) {
                    if (field.getType().isAssignableFrom(Map.class)) {
                        field.setAccessible(true);
                        try {
                            Map<?, ?> o1 = (Map<?, ?>) field.get(o);
                            for (Map.Entry<?, ?> objectObjectEntry : o1.entrySet()) {
                                result.put(field.getName() + "." + objectObjectEntry.getKey(),
                                        objectObjectEntry.getValue() == null ? "" : objectObjectEntry.getValue().toString());
                            }
                        } catch (IllegalAccessException e) {
                            result.put(field.getName(), "undefined");
                        }
                    } else if (!field.getType().isLocalClass()) {
                        field.setAccessible(true);
                        try {
                            result.put(field.getName(), field.get(o) == null ? null : field.get(o).toString());
                        } catch (IllegalAccessException e) {
                            result.put(field.getName(), "undefined");
                        }
                    } else {
                        for (Field field1 : field.getType().getDeclaredFields()) {
                            field1.setAccessible(true);
                            try {
                                result.put(field1.getName(), field1.get(o) == null ? null : field1.get(o).toString());
                            } catch (IllegalAccessException e) {
                                result.put(field.getName() + "." + field1.getName(), "undefined");
                            }
                        }
                    }
                }
            }
            current = current.getSuperclass();
        }
        return result;
    }
}


