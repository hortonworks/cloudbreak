package com.sequenceiq.environment;

import static org.reflections.scanners.Scanners.SubTypes;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

public class ConstructorTest {

    @Test
    @DisplayName("all request/response classes should have constructor implemented")
    public void testIfConstructorIsImplementedOnRequestResponses() throws ClassNotFoundException {
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackage("com.sequenceiq.environment").setScanners(Scanners.SubTypes));
        Set<String> missingConstructors = new HashSet<>();
        for (String clazz : reflections.getAll(SubTypes)) {
            if (clazz.startsWith("com.sequenceiq.environment") && !isTestClass(clazz)
                    && (requestOrResponse(clazz) && !clazz.contains(".Valid"))
                    && !Class.forName(clazz).isInterface()) {
                try {
                    Constructor<?> declaredConstructor = Class.forName(clazz).getDeclaredConstructor();
                    if (declaredConstructor == null || !declaredConstructor.getDeclaringClass().getName().equals(clazz)) {
                        missingConstructors.add(clazz);
                    }
                } catch (Exception e) {
                    missingConstructors.add(clazz);
                }
            }
        }
        Assertions.assertEquals(0, missingConstructors.size(),
                String.format("These classes do not have constructor(): %s. Please add that", String.join(",\n", missingConstructors)));
    }

    private boolean requestOrResponse(String clazz) {
        return clazz.endsWith("Request") || clazz.endsWith("Response") || clazz.endsWith("Parameters");
    }

    private boolean isTestClass(String clazz) {
        return clazz.endsWith("Test") || clazz.contains("Test$");
    }

}
