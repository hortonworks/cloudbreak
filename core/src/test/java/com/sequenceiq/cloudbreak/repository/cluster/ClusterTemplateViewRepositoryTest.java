package com.sequenceiq.cloudbreak.repository.cluster;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class ClusterTemplateViewRepositoryTest {

    private static final String FIND_ALL_USER_MANAGED_AND_DEFAULT_BY_ENVIRONMENT_CRN_METHOD_NAME = "findAllUserManagedAndDefaultByEnvironmentCrn";

    private static final String FIND_ALL_ACTIVE_METHOD_NAME = "findAllActive";

    private static final Set<Method> REPOSITORY_METHODS = Arrays.asList(ClusterTemplateViewRepository.class.getDeclaredMethods())
            .stream()
            .collect(Collectors.toSet());

    @Test
    void testFindAllUserManagedAndDefaultByEnvironmentCrnContainsFilteringForEnvCrn() {
        String query = getQueryAnnotationContentForMethod(FIND_ALL_USER_MANAGED_AND_DEFAULT_BY_ENVIRONMENT_CRN_METHOD_NAME);

        assertTrue(query.contains("s.environmentCrn= :environmentCrn OR c.status = 'DEFAULT'"));
    }

    @Test
    void testFindAllUserManagedAndDefaultByEnvironmentCrnContainsFilteringForWorkspaceId() {
        String query = getQueryAnnotationContentForMethod(FIND_ALL_USER_MANAGED_AND_DEFAULT_BY_ENVIRONMENT_CRN_METHOD_NAME);

        assertTrue(query.contains("workspace.id= :workspaceId"));
    }

    @Test
    void testFindAllUserManagedAndDefaultByEnvironmentCrnContainsFilteringForNotDeletedResults() {
        String query = getQueryAnnotationContentForMethod(FIND_ALL_USER_MANAGED_AND_DEFAULT_BY_ENVIRONMENT_CRN_METHOD_NAME);

        assertTrue(query.contains("status <> 'DEFAULT_DELETED'"));
    }

    @Test
    void testFindAllActiveContainsFilterForWorkspaceId() {
        String query = getQueryAnnotationContentForMethod(FIND_ALL_ACTIVE_METHOD_NAME);

        assertTrue(query.contains("workspace.id= :workspaceId"));
    }

    @Test
    void testFindAllActiveContainsFilterForNotDeletedResults() {
        String query = getQueryAnnotationContentForMethod(FIND_ALL_ACTIVE_METHOD_NAME);

        assertTrue(query.contains("AND b.status <> 'DEFAULT_DELETED'"));
    }

    private String getQueryAnnotationContentForMethod(String methodName) {
        Optional<Method> findAllMethod = REPOSITORY_METHODS.stream()
                .filter(method -> methodName.equals(method.getName()))
                .findFirst();
        if (findAllMethod.isPresent()) {
            Query queryAnnotation = findAllMethod.get().getAnnotation(Query.class);
            if (queryAnnotation != null) {
                return queryAnnotation.value();
            } else {
                throw new IllegalStateException("Method (\"" + methodName + "\") has no Query annotation that is required for the test!");
            }
        } else {
            throw new IllegalStateException("Unable to find repository method called \"" + methodName + "\"");
        }
    }

}