package com.sequenceiq.environment.testing;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

public class Junit4Scanner {

    @Test
    void checkIllegalJUnit4Usage() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        List<String> errorMessages = new ArrayList<>();
        for (ClassInfo info : ClassPath.from(loader).getTopLevelClasses()) {
            if (info.getName().startsWith("com.sequenceiq.environment.") && info.getName().endsWith("Test")) {
                Class<?> clazz = info.load();
                checkRunWith(clazz, errorMessages);
                checkTestMethods(clazz, errorMessages);
                checkRuleFields(clazz, errorMessages);
            }
        }
        if (!errorMessages.isEmpty()) {
            throw new IllegalStateException(String.format("Found %d forbidden JUnit4-related annotations:%n%s",
                    errorMessages.size(), String.join("\n", errorMessages)));
        }
    }

    private void checkRunWith(Class<?> clazz, List<String> errorMessages) {
        List<Annotation> annotations = Arrays.asList(clazz.getAnnotations());
        findJunit4Annotation(org.junit.runner.RunWith.class, clazz.getName(), annotations, errorMessages);
    }

    private void checkTestMethods(Class<?> clazz, List<String> errorMessages) {
        Set<Annotation> annotations = Arrays.stream(clazz.getDeclaredMethods())
                .flatMap(m -> Arrays.stream(m.getAnnotations())).collect(Collectors.toSet());
        findJunit4Annotation(org.junit.After.class, clazz.getName(), annotations, errorMessages);
        findJunit4Annotation(org.junit.AfterClass.class, clazz.getName(), annotations, errorMessages);
        findJunit4Annotation(org.junit.Before.class, clazz.getName(), annotations, errorMessages);
        findJunit4Annotation(org.junit.BeforeClass.class, clazz.getName(), annotations, errorMessages);
        findJunit4Annotation(org.junit.Ignore.class, clazz.getName(), annotations, errorMessages);
        findJunit4Annotation(org.junit.Rule.class, clazz.getName(), annotations, errorMessages);
        findJunit4Annotation(org.junit.Test.class, clazz.getName(), annotations, errorMessages);
    }

    private void checkRuleFields(Class<?> clazz, List<String> errorMessages) {
        Set<Annotation> annotations = Arrays.stream(clazz.getDeclaredFields())
                .flatMap(m -> Arrays.stream(m.getAnnotations())).collect(Collectors.toSet());
        findJunit4Annotation(org.junit.Rule.class, clazz.getName(), annotations, errorMessages);
    }

    private void findJunit4Annotation(Class<?> annotationClass, String className, Collection<Annotation> annotations, List<String> errorMessages) {
        annotations.stream()
                .filter(a -> annotationClass.equals(a.annotationType()))
                .findAny()
                .ifPresent(a -> errorMessages.add(String.format("@%s in %s", annotationClass.getName(), className)));
    }

}
