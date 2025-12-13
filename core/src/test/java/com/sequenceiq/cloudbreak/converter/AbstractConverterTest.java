package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.google.common.collect.ObjectArrays;
import com.sequenceiq.cloudbreak.TestException;

public class AbstractConverterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConverterTest.class);

    public void assertAllFieldsNotNull(Object obj) {
        assertAllFieldsNotNull(obj, Collections.emptyList());
    }

    public void assertAllFieldsNotNull(Object obj, List<String> skippedFields) {
        Field[] fields = obtainFields(obj);
        int count = 0;
        int skippedCount = 0;
        Set<String> remainingFields = new HashSet<>(skippedFields);
        remainingFields.remove("workspace");
        remainingFields.remove("id");
        Set<String> missing = new HashSet<>();
        for (Field field : fields) {
            if (!skippedFields.contains(field.getName())) {
                if (isFieldNull(obj, field)) {
                    missing.add(field.getName());
                }
                count++;
            } else {
                skippedCount++;
            }
            remainingFields.remove(field.getName());
        }
        assertTrue(remainingFields.isEmpty(), "Field(s) \"" + String.join("\", \"", remainingFields) + "\" does not exist in class anymore.");
        LOGGER.info("Checked fields count: {}, skipped counts: {}", count, skippedCount);
        assertTrue(missing.isEmpty(), "Field(s) \"" + String.join("\", \"", missing) + "\" is null.");
    }

    private boolean isFieldNull(Object obj, Field field) {
        try {
            ReflectionUtils.makeAccessible(field);
            return field.get(obj) == null;
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
