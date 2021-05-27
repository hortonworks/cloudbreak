package com.sequenceiq.redbeams;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import com.sequenceiq.cloudbreak.common.event.Acceptable;

class FlowPayloadConstructorTest {

    @Test
    void constructorAccessible() {
        Reflections reflections = new Reflections("com.sequenceiq", new SubTypesScanner(true));
        Set<Class<? extends Acceptable>> eventClasses = reflections.getSubTypesOf(Acceptable.class);
        eventClasses.stream()
                .filter(c -> !c.isInterface() && !isAbstract(c) && !c.isAnonymousClass() && !c.isLocalClass() && !c.isMemberClass())
                .filter(c -> !c.getName().endsWith("Test"))
                .forEach(this::checkForConstructor);
    }

    private void checkForConstructor(Class<? extends Acceptable> clazz) {
        Set<Constructor> constructors = ReflectionUtils.getConstructors(clazz, this::isPublic);
        assertFalse(constructors.isEmpty(), String.format("%s class has no visible public constructor!", clazz.getName()));
    }

    private boolean isPublic(Constructor<?> c) {
        return Modifier.isPublic(c.getModifiers());
    }

    private boolean isAbstract(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers());
    }
}
