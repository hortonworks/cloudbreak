package com.sequenceiq.cloudbreak.repository;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.util.ReflectionUtils;

public abstract class BaseRepositoryQueryDeletionTest {

    protected Map<String, String> collectMethodsWhichAreNotAnnotated(Class baseClazz) {
        Reflections reflections = new Reflections("com.sequenceiq", new SubTypesScanner(false));
        Set<Class<? extends Class>> subTypesOf = reflections.getSubTypesOf(baseClazz);
        Map<String, String> missingQueryAnnotation = new HashMap<>();
        for (Class clazz : subTypesOf) {
            try {
                if (clazz.isInterface()) {
                    for (Method method : collectDeleteMethods(clazz)) {
                        if (!method.isAnnotationPresent(Query.class) || !method.isAnnotationPresent(Modifying.class)) {
                            missingQueryAnnotation.put(clazz.getName(), method.getName());
                        }
                    }
                }
            } catch (Exception e) {
                missingQueryAnnotation.put(clazz.getName(), null);
            }
        }
        return missingQueryAnnotation;
    }

    protected String getErrorMessage(Map<String, String> missingQueryAnnotation) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : missingQueryAnnotation.entrySet()) {
            stringBuilder.append(entry.getKey() + " class has a method " + entry.getValue()
                    + " which has no specific @Query and @Modifying annotions.\n");
        }
        return stringBuilder.toString();
    }

    protected Set<Method> collectDeleteMethods(Class clazz) {
        return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz))
                .filter(m -> m.getDeclaringClass().getName().contains(clazz.getName()))
                .filter(m -> m.getName().toLowerCase(Locale.ROOT).startsWith("delete"))
                .collect(Collectors.toSet());
    }
}
