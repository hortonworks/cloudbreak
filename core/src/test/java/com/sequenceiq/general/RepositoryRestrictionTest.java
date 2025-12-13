package com.sequenceiq.general;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

class RepositoryRestrictionTest {

    private static final String BASE_PATH = "com.sequenceiq";

    private static final String CLOUDBREAK_BASE_PACKAGE = BASE_PATH + ".cloudbreak";

    private static final String DISTROX_BASE_PACKAGE = BASE_PATH + ".distrox";

    @ParameterizedTest
    @SuppressWarnings("unchecked")
    @MethodSource("testDataProvider")
    @DisplayName("Testing if core module's components are using the repositories trough delegated method(s) from the related service")
    void testForSingleRepositoryUsage(String basePath) {
        Set<Class<? extends Repository>> repos = getRepos(basePath);
        Set<Class<?>> compos = getServicesAndComponents(basePath);
        repos.forEach(repo -> {
            AtomicLong count = new AtomicLong(0);
            Set<String> compoNames = new LinkedHashSet<>();
            compos.forEach(service -> {
                Set<? extends Class<?>> injectedFields = ReflectionUtils.getFields(service)
                        .stream()
                        .filter(this::isFieldInjected)
                        .map(Field::getType)
                        .collect(toSet());
                if (injectedFields.stream().anyMatch(fieldClass -> fieldClass.equals(repo))) {
                    count.addAndGet(1L);
                    compoNames.add(service.getSimpleName());
                }
            });
            assertTrue(count.get() <= 1, getExceptionMessage(repo.getSimpleName(), compoNames));
        });
    }

    private static Stream<Arguments> testDataProvider() {
        return Stream.of(
                Arguments.of(CLOUDBREAK_BASE_PACKAGE),
                Arguments.of(DISTROX_BASE_PACKAGE)
        );
    }

    private Set<Class<? extends Repository>> getRepos(String basePathForRepos) {
        Reflections reflections = new Reflections(basePathForRepos);
        return reflections.getSubTypesOf(Repository.class);
    }

    private Set<Class<?>> getServicesAndComponents(String basePathForComponentsAndServices) {
        return getServicesAndComponents(basePathForComponentsAndServices, Service.class, Component.class);
    }

    @SafeVarargs
    private Set<Class<?>> getServicesAndComponents(String basePath, Class<? extends Annotation>... annotationClasses) {
        Reflections reflections = new Reflections(basePath);
        Set<Class<?>> servicesAndComponents = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotationClass : annotationClasses) {
            servicesAndComponents.addAll(reflections.getTypesAnnotatedWith(annotationClass));
        }
        return servicesAndComponents;
    }

    private boolean isFieldInjected(Field field) {
        return Arrays.stream(field.getAnnotations()).anyMatch(annotation -> Inject.class.equals(annotation.annotationType()));
    }

    private String getExceptionMessage(String repoName, Set<String> affectedClasses) {
        String listedClasses = String.join(", ", affectedClasses);
        String initiativeMessage = "Repositories should've injected in the related service class and nowhere else!";
        return String.format("%s %nRepository (%s) has injected more than one time in the following classes: %s", initiativeMessage, repoName, listedClasses);
    }

}
