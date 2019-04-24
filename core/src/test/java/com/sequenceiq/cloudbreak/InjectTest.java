package com.sequenceiq.cloudbreak;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

public class InjectTest {

    @Test
    public void testIfThereAreUnusedInjections() {
        Reflections reflections = new Reflections("com.sequenceiq",
                new FieldAnnotationsScanner(),
                new TypeAnnotationsScanner(),
                new SubTypesScanner(),
                new MemberUsageScanner());

        Map<String, Set<String>> unusedFields = new HashMap<>();
        reflections.getFieldsAnnotatedWith(Inject.class).forEach(field -> {
            try {
                Set<Member> usages = reflections.getFieldUsage(field);
                if (usages.isEmpty()) {
                    String className = field.getDeclaringClass().getSimpleName();
                    unusedFields.computeIfAbsent(className, key -> new HashSet<>()).add(field.getName());
                }
            } catch (RuntimeException e) {
                // ignore if cannot check fields
            }
        });

        Assert.assertTrue(
                String.format("Classes with unused injected fields: %s", String.join(", ", unusedFields.keySet())), unusedFields.isEmpty());
    }

}
