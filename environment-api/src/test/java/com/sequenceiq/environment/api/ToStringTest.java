package com.sequenceiq.environment.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.springframework.util.ReflectionUtils;

public class ToStringTest {

    @Test
    @DisplayName("all request/response classes should have toString implemented")
    public void testIfToStringIsImplementedOnRequestResponses() throws ClassNotFoundException {
        Reflections reflections = new Reflections("com.sequenceiq.environment.api", new SubTypesScanner(false));
        Set<String> missingToStrings = new HashSet<>();
        for (String clazz : reflections.getAllTypes()) {
            if (!clazz.endsWith("Test")
                    && requestOrResponse(clazz) && !clazz.contains(".Valid")
                    && !Class.forName(clazz).isInterface()) {
                try {
                    Class<?> type = Class.forName(clazz);
                    Method toString = ReflectionUtils.findMethod(type, "toString");
                    if (toString == null || Object.class.equals(toString.getDeclaringClass())) {
                        missingToStrings.add(clazz);
                    }
                } catch (Exception e) {
                    missingToStrings.add(clazz);
                }
            }
        }
        assertEquals(0, missingToStrings.size(),
                String.format("These classes inherit only Object.toString(): %s. Implement or inherit a custom toString().",
                        String.join(",\n", missingToStrings)));
    }

    private boolean requestOrResponse(String clazz) {
        return clazz.endsWith("Request") || clazz.endsWith("Response") || clazz.endsWith("Params") || clazz.endsWith("Parameters");
    }

}
