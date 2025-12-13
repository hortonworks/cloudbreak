package com.sequenceiq.environment.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.reflections.scanners.Scanners.SubTypes;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

public class ConstructorTest {

    @Test
    @DisplayName("all request/response classes should have constructor implemented")
    public void testIfConstructorIsImplementedOnRequestResponses() throws ClassNotFoundException {
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackage("com.sequenceiq.environment.api").setScanners(Scanners.SubTypes));
        Set<String> missingConstructors = new HashSet<>();
        for (String clazz : reflections.getAll(SubTypes)) {
            if (!clazz.endsWith("Test")
                    && requestOrResponse(clazz) && !clazz.contains(".Valid")
                    && !Class.forName(clazz).isInterface() && !Class.forName(clazz).isRecord()) {
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
        assertEquals(0, missingConstructors.size(),
                String.format("These classes do not have constructor(): %s. Please add that", String.join(",\n", missingConstructors)));
    }

    private boolean requestOrResponse(String clazz) {
        return (clazz.endsWith("Request") || clazz.endsWith("Response") || clazz.endsWith("Params") || clazz.endsWith("Parameters"))
                && !clazz.contains("GeneralCollectionV4Response");
    }

}
