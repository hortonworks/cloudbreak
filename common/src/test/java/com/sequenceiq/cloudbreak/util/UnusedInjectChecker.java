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

import com.google.common.base.Strings;

public class UnusedInjectChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnusedInjectChecker.class);

    public void check() {
        Reflections reflections = new Reflections("com.sequenceiq",
                Scanners.FieldsAnnotated,
                Scanners.TypesAnnotated,
                Scanners.SubTypes,
                new MemberUsageScanner());
        reflections.getStore().forEach((scanner, values) -> LOGGER.info("Found {} entries for scanner type {}", values.size(), scanner));

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
            } catch (RuntimeException ignored) {
                // ignore if cannot check fields
            }
        });

        Set<String> classesWithUnusedInjectedFields = new HashSet<>();

        unusedFields.forEach((key, value) -> {
            classesWithUnusedInjectedFields.add("=> " + key + ':' + lineSeparator() + "\t-> " + String.join(lineSeparator() + "\t-> ", value));
        });
        if (!unusedFields.isEmpty()) {
            String format = String.format("Classes with unused injected fields:%s%s",
                    lineSeparator(), String.join(Strings.repeat(lineSeparator(), 2), classesWithUnusedInjectedFields));
            LOGGER.error(format);
            //TODO Disabled, because this is still flaky. The `MemberUsageScanner` doesn't seem to always find all the usages.
//            fail(format);
        }
    }

}
