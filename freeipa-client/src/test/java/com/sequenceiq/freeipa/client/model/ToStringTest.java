package com.sequenceiq.freeipa.client.model;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.springframework.util.ReflectionUtils;

public class ToStringTest {

    @Test
    @DisplayName("all model classes shoud have toString implemented")
    public void testIfToStringIsImplemented() throws ClassNotFoundException {
        Reflections reflections = new Reflections("com.sequenceiq.freeipa.client.model", new SubTypesScanner(false));
        for (String clazz : reflections.getAllTypes()) {
            if (!clazz.endsWith("Test")) {
                Method toString = ReflectionUtils.findMethod(Class.forName(clazz), "toString");
                assertEquals(clazz, toString.getDeclaringClass().getName());
            }
        }
    }

}
