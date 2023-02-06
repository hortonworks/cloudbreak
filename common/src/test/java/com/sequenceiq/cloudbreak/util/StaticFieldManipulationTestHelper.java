package com.sequenceiq.cloudbreak.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.springframework.util.ReflectionUtils;

public class StaticFieldManipulationTestHelper {

    private StaticFieldManipulationTestHelper() {
    }

    public static void setFinalStatic(Field field, Object newValue) throws Exception {
        ReflectionUtils.makeAccessible(field);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        ReflectionUtils.makeAccessible(modifiersField);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }
}
