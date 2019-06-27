package com.sequenceiq.environment.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class BuilderFieldValidator {

    public void assertBuilderFields(Class<?> entityClass, Class<?> builderClass) {
        Set<String> builderFields = Arrays.stream(builderClass.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
        Set<String> entityFields = Arrays.stream(entityClass.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
        Set<String> diff = new HashSet<>(entityFields);
        diff.removeAll(builderFields);

        assertThat(builderFields.size())
                .withFailMessage("Builder does not propagate fields: [%s]", String.join(", ", diff))
                .isEqualTo(entityFields.size());
    }
}
