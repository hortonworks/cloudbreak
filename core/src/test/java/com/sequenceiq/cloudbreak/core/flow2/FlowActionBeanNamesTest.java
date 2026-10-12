package com.sequenceiq.cloudbreak.core.flow2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;

class FlowActionBeanNamesTest {

    private static final String FLOW_PACKAGE = "com.sequenceiq.cloudbreak.core.flow2";

    @Test
    void explicitlyNamedBeansAreUniqueAcrossAllFlows() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Configuration.class));

        List<String> beanNames = new java.util.ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(FLOW_PACKAGE)) {
            Class<?> configuration = Class.forName(definition.getBeanClassName());
            Arrays.stream(configuration.getDeclaredMethods())
                    .map(method -> method.getAnnotation(Bean.class))
                    .filter(bean -> bean != null && bean.name().length > 0)
                    .forEach(bean -> beanNames.addAll(Arrays.asList(bean.name())));
        }

        Map<String, Long> duplicates = beanNames.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(beanNames).isNotEmpty();
        assertThat(duplicates).as("flow action bean names must be unique, otherwise the service fails to start").isEmpty();
    }
}
