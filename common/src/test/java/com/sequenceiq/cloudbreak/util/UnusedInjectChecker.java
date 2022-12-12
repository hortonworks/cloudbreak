package com.sequenceiq.cloudbreak.util;

import static java.lang.System.lineSeparator;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.reflections.Reflections;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.Scanners;

public class UnusedInjectChecker {

    public void check() {
        Reflections reflections = new Reflections("com.sequenceiq",
                Scanners.FieldsAnnotated,
                new MemberUsageScanner());

        Map<String, Set<String>> unusedFields = new HashMap<>();
        reflections.getFieldsAnnotatedWith(Inject.class).forEach(field -> {
            try {
                Set<String> usages = reflections.getStore().getOrDefault(MemberUsageScanner.class.getSimpleName(), Collections.emptyMap())
                        .getOrDefault(reflections.toName((AnnotatedElement) field), Collections.emptySet());
                if (usages.isEmpty()) {
                    String className = field.getDeclaringClass().getName();
                    unusedFields.computeIfAbsent(className, key -> new HashSet<>()).add(field.toString());
                }
            } catch (RuntimeException e) {
                // ignore if cannot check fields
            }
        });

        Set<String> fields = new HashSet<>();

        unusedFields.forEach((key, value) -> {
            fields.add(key + ": " + String.join(", ", value));
        });
        if (!unusedFields.isEmpty()) {
            fail(String.format("Classes with unused injected fields: %s%s", lineSeparator(), String.join(lineSeparator(), fields)));
        }
    }

}
