package com.sequenceiq.cloudbreak;

import static java.lang.System.lineSeparator;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.Convert;

import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

class DefaultEnumConverterCompatibilityTest {

    @Test
    void testDefaultEnumConverterCompatibility() {
        Reflections reflections = new Reflections("com.sequenceiq",
                new FieldAnnotationsScanner());

        Map<String, Set<String>> incompatibleFields = new HashMap<>();
        reflections.getFieldsAnnotatedWith(Convert.class).forEach(field -> {
            try {
                Optional<Type> defaultEnumConverterType = Optional.of((field.getAnnotation(Convert.class).converter()).getGenericSuperclass())
                        .filter(t -> ((ParameterizedType) t).getRawType().getTypeName().equals(DefaultEnumConverter.class.getTypeName()));

                if (defaultEnumConverterType.isPresent()) {
                    boolean hasCompatibleGenericParameter = Stream.of(((ParameterizedType) defaultEnumConverterType.get()).getActualTypeArguments())
                            .allMatch(a -> a.getTypeName().equals(field.getType().getTypeName()));

                    if (!hasCompatibleGenericParameter) {
                        String className = field.getDeclaringClass().getName();
                        incompatibleFields.computeIfAbsent(className, key -> new HashSet<>()).add(field.toString());
                    }
                }
            } catch (RuntimeException e) {
                // ignore if cannot check fields
            }
        });

        Set<String> fields = new HashSet<>();

        incompatibleFields.forEach((key, value) -> {
            fields.add(key + ": " + String.join(", ", value));
        });

        assertTrue(incompatibleFields.isEmpty(), String.format("Classes with incompatible DefaultEnumConverter: %s%s", lineSeparator(),
                        String.join(lineSeparator(), fields)));
    }
}
