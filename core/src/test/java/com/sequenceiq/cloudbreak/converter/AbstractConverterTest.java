package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ObjectArrays;
import com.sequenceiq.cloudbreak.TestException;

public class AbstractConverterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConverterTest.class);

    public void assertAllFieldsNotNull(Object obj) {
        Field[] fields = obtainFields(obj);
        for (Field field : fields) {
            assertFieldNotNull(obj, field);
        }
    }

    public void assertAllFieldsNotNull(Object obj, List<String> skippedFields) {
        Field[] fields = obtainFields(obj);
        int count = 0;
        int skippedCount = 0;
        for (Field field : fields) {
            if (!skippedFields.contains(field.getName())) {
                assertFieldNotNull(obj, field);
                count++;
            } else {
                skippedCount++;
            }
        }
        LOGGER.info("Checked fields count: {}, skipped counts: {}", count, skippedCount);
    }

    private void assertFieldNotNull(Object obj, Field field) {
        try {
            field.setAccessible(true);
            assertNotNull("Field '" + field.getName() + "' is null.", field.get(obj));
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new TestException(e.getMessage());
        }
    }

    private Field[] obtainFields(Object obj) {
        Field[] fields = obj.getClass().getDeclaredFields();
        Field[] parentFields = obj.getClass().getSuperclass().getDeclaredFields();
        return ObjectArrays.concat(fields, parentFields, Field.class);
    }
}
