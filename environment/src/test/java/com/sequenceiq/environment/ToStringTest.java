package com.sequenceiq.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.auth.security.AuthResource;

public class ToStringTest {

    @Test
    @DisplayName("all model classes should have toString implemented")
    public void testIfToStringIsImplementedOnAuthResources() throws ClassNotFoundException {
        Reflections reflections = new Reflections("com.sequenceiq.environment", new SubTypesScanner(false));
        Set<Class<? extends AuthResource>> subTypesOf = reflections.getSubTypesOf(AuthResource.class);
        Set<String> missingToStrings = new HashSet<>();
        for (Class clazz : subTypesOf) {
            try {
                if (!clazz.isInterface()) {
                    Method toString = ReflectionUtils.findMethod(clazz, "toString");
                    if (!clazz.getName().equals(toString.getDeclaringClass().getName())) {
                        missingToStrings.add(clazz.getName());
                    }
                }
            } catch (Exception e) {
                missingToStrings.add(clazz.getName());
            }
        }
        assertEquals(0, missingToStrings.size(),
                String.format("These classes do not have toString(): %s. Please add that", String.join(",\n", missingToStrings)));
    }

    @Test
    @DisplayName("all DTO classes should have toString implemented")
    public void testIfToStringIsImplementedOnDTOResources() throws ClassNotFoundException {
        Reflections reflections = new Reflections("com.sequenceiq.environment", new SubTypesScanner(false));
        Set<String> missingToStrings = new HashSet<>();
        for (String clazz : reflections.getAllTypes()) {
            if (clazz.endsWith("Dto")) {
                try {
                    if (!Class.forName(clazz).isInterface()) {
                        Method toString = ReflectionUtils.findMethod(Class.forName(clazz), "toString");
                        if (!clazz.equals(toString.getDeclaringClass().getName())) {
                            missingToStrings.add(clazz);
                        }
                    }
                } catch (Exception e) {
                    missingToStrings.add(clazz);
                }
            }
        }
        assertEquals(0, missingToStrings.size(),
                String.format("These classes do not have toString(): %s. Please add that", String.join(",\n", missingToStrings)));
    }

}
