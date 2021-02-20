package com.sequenceiq.environment.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class BuilderFieldValidator {

    public void assertBuilderFields(Class<?> entityClass, Class<?> builderClass) {
        // We don't want to have setters for static fields
        Set<String> entityFields = getNonStaticFields(entityClass);
        Set<String> builderFields = getNonStaticFields(builderClass);
        Set<String> diff = new HashSet<>(entityFields);
        diff.removeAll(builderFields);

        assertThat(builderFields.size())
                .withFailMessage("Builder does not propagate fields: [%s]", String.join(", ", diff))
                .isEqualTo(entityFields.size());
    }

    private Set<String> getNonStaticFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getName).collect(Collectors.toSet());
    }
}
