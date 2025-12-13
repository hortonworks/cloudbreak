package com.sequenceiq.freeipa.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import com.google.common.reflect.ClassPath;

public class ToStringTest {

    @Test
    @DisplayName("all model classes shoud have toString implemented")
    public void testIfToStringIsImplemented() throws ClassNotFoundException, IOException {
        Set<? extends Class<?>> classes = ClassPath.from(ClassLoader.getSystemClassLoader())
                .getAllClasses()
                .stream()
                .filter(clazz -> clazz.getPackageName()
                        .equalsIgnoreCase("com.sequenceiq.freeipa.client.model"))
                .filter(clazz -> !clazz.getName().endsWith("Test"))
                .map(clazz -> clazz.load())
                .filter(clazz -> !clazz.isEnum())
                .collect(Collectors.toSet());
        for (Class<?> clazz : classes) {
            if (!clazz.getName().endsWith("Test")) {
                Method toString = ReflectionUtils.findMethod(clazz, "toString");
                assertNotNull(toString, clazz + " toString method is null");
                assertEquals(clazz.getName(), toString.getDeclaringClass().getName());
            }
        }
    }

}
