package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;
import java.util.List;

import com.google.common.collect.ObjectArrays;

public abstract class AbstractEntityConverterTest<S> {
    private S source = createSource();

    public abstract S createSource();

    public S getSource() {
        return this.source;
    }

    public void assertAllFieldsNotNull(Object obj) {
        Field[] fields = obtainFields(obj);
        for (Field field : fields) {
            assertFieldNotNull(obj, field);
        }
    }

    public void assertAllFieldsNotNull(Object obj, List<String> skippedFields) {
        Field[] fields = obtainFields(obj);
        for (Field field : fields) {
            if (!skippedFields.contains(field.getName())) {
                assertFieldNotNull(obj, field);
            }
        }
    }

    private void assertFieldNotNull(Object obj, Field field) {
        try {
            field.setAccessible(true);
            assertFalse("Field '" + field.getName() + "' is null.", field.get(obj) == null);
        } catch (IllegalAccessException|IllegalArgumentException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private Field[] obtainFields(Object obj) {
        Field[] fields = obj.getClass().getDeclaredFields();
        Field[] parentFields = obj.getClass().getSuperclass().getDeclaredFields();
        return ObjectArrays.concat(fields, parentFields, Field.class);
    }
}
