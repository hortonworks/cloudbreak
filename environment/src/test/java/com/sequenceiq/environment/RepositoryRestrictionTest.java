package com.sequenceiq.environment;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

class RepositoryRestrictionTest {

    private static final String BASE_PATH = "com.sequenceiq";

    private static final String ENVIRONMENT_BASE_PACKAGE = BASE_PATH + ".environment";

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Testing if environment module's components are using the repositories trough delegated method(s) from the related service")
    void testForSingleRepositoryUsage() {
        Set<Class<? extends Repository>> repos = getRepos();
        Set<Class<?>> compos = getServicesAndComponents();
        repos.forEach(repo -> {
            AtomicLong count = new AtomicLong(0);
            Set<String> compoNames = new LinkedHashSet<>();
            compos.forEach(service -> {
                Set<? extends Class<?>> injectedFields = ReflectionUtils.getFields(service)
                        .stream()
                        .map(Field::getType)
                        .collect(toSet());
                if (injectedFields.stream().anyMatch(fieldClass -> fieldClass.equals(repo))) {
                    count.addAndGet(1L);
                    compoNames.add(service.getSimpleName());
                }
            });

            if (count.get() > 1) {
                // Then it should be ensured that a Repository can be injected only to Services that has the same name prefix
                // e.g. CredentialRepository can be injected only to Credential****Service

                String allowedNamesRegexp = repo.getSimpleName().replace("Repository", ".*Service");
                Set<String> filteredNames = compoNames.stream().filter(name -> !name.matches(allowedNamesRegexp)).collect(toSet());

                assertTrue(filteredNames.isEmpty(), getExceptionMessage(repo.getSimpleName(), compoNames));
            } else {
                assertTrue(count.get() <= 1, getExceptionMessage(repo.getSimpleName(), compoNames));
            }
        });
    }

    private Set<Class<? extends Repository>> getRepos() {
        Reflections reflections = new Reflections(RepositoryRestrictionTest.ENVIRONMENT_BASE_PACKAGE);
        return reflections.getSubTypesOf(Repository.class);
    }

    private Set<Class<?>> getServicesAndComponents() {
        return getServicesAndComponents(Service.class, Component.class);
    }

    @SafeVarargs
    private Set<Class<?>> getServicesAndComponents(Class<? extends Annotation>... annotationClasses) {
        Reflections reflections = new Reflections(RepositoryRestrictionTest.ENVIRONMENT_BASE_PACKAGE);
        Set<Class<?>> servicesAndComponents = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotationClass : annotationClasses) {
            servicesAndComponents.addAll(reflections.getTypesAnnotatedWith(annotationClass));
        }
        return servicesAndComponents;
    }

    private String getExceptionMessage(String repoName, Set<String> affectedClasses) {
        String listedClasses = String.join(", ", affectedClasses);
        String initiativeMessage = "Repositories should've been injected in the related service class and nowhere else!";
        return String.format("%s %nRepository (%s) has been injected more than one time in the following classes: %s",
                initiativeMessage, repoName, listedClasses);
    }

}
