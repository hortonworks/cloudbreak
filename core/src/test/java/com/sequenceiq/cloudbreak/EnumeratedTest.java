package com.sequenceiq.cloudbreak;

import static java.lang.System.lineSeparator;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Enumerated;

import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;

class EnumeratedTest {

    @Test
    void testIfThereAreEnumeratedAnnotations() {
        Reflections reflections = new Reflections("com.sequenceiq",
                new FieldAnnotationsScanner());

        Map<String, Set<String>> enumeratedFields = new HashMap<>();
        reflections.getFieldsAnnotatedWith(Enumerated.class).forEach(field -> {
            try {
                String className = field.getDeclaringClass().getName();
                enumeratedFields.computeIfAbsent(className, key -> new HashSet<>()).add(field.toString());
            } catch (RuntimeException e) {
                // ignore if cannot check fields
            }
        });

        Set<String> fields = new HashSet<>();

        enumeratedFields.forEach((key, value) -> {
            fields.add(key + ": " + String.join(", ", value));
        });

        assertTrue(enumeratedFields.isEmpty(), String.format("Classes with @Enumerated fields: %s%s%s%s", lineSeparator(),
                String.join(lineSeparator(), fields), lineSeparator(), "Use @Converter instead of @Enumerated"));
    }
}
