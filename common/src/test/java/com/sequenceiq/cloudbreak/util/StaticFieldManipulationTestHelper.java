package com.sequenceiq.cloudbreak.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.util.ReflectionUtils;

public class StaticFieldManipulationTestHelper {

    private StaticFieldManipulationTestHelper() {
    }

    public static void setFinalStatic(Field field, Object newValue) throws Exception {
        // TODO: rewrite it!!!
        // It is an ugly and fragile hack and it needs opening Java internal modules with
        // --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util.concurrent=ALL-UNNAMED
        Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
        getDeclaredFields0.setAccessible(true);
        Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
        Field modifiersField = null;
        for (Field each : fields) {
            if ("modifiers".equals(each.getName())) {
                modifiersField = each;
                break;
            }
        }
        modifiersField.setAccessible(true);
        ReflectionUtils.makeAccessible(field);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }
}
