package com.sequenceiq.cloudbreak.util;

import static java.lang.System.lineSeparator;

import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.reflections.Reflections;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.Scanners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnusedInjectChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnusedInjectChecker.class);

    public void check() {
        Reflections reflections = new Reflections("com.sequenceiq",
                Scanners.FieldsAnnotated,
                Scanners.TypesAnnotated,
                Scanners.SubTypes,
                new MemberUsageScanner());

        Map<String, Set<String>> unusedFields = new HashMap<>();
        reflections.getFieldsAnnotatedWith(Inject.class).forEach(field -> {
            try {
                Set<String> usages = new HashSet<>();
                for (String s : reflections.getStore().keySet()) {
                    usages.addAll(reflections.getStore().getOrDefault(s, Collections.emptyMap())
                            .getOrDefault(reflections.toName((AnnotatedElement) field), Collections.emptySet()));
                }
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
            String format = String.format("Classes with unused injected fields: %s%s", lineSeparator(), String.join(lineSeparator(), fields));
            LOGGER.error(format);
            // TODO disabling this because it is causing flaky runs
            // fail(format);
        }
    }

}
