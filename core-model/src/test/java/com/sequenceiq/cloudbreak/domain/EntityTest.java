package com.sequenceiq.cloudbreak.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.workspace.model.TenantAwareResource;

class EntityTest {
    @Test
    void testIfClassesWithSecretFieldsAreInheritedFromTenantOrWorkspaceOrAccountIdAwareResources() {
        Reflections reflections = new Reflections("com.sequenceiq");
        Set<Class<?>> entityClasses = reflections.getTypesAnnotatedWith(Entity.class);
        Set<Class<?>> wrongClasses = entityClasses.stream()
                .filter(cls -> FieldUtils.getFieldsWithAnnotation(cls, SecretValue.class).length > 0)
                .filter(cls -> !TenantAwareResource.class.isAssignableFrom(cls))
                .filter(cls -> !AccountIdAwareResource.class.isAssignableFrom(cls))
                .collect(Collectors.toSet());
        assertTrue(wrongClasses.isEmpty(), String.format("Classes with Secret fields should be inherited from TenantAwareResource, WorkspaceAwareResource " +
                        "or AccountIdAwareResource. Wrong classes: %s",
                wrongClasses.stream().map(Class::getName).collect(Collectors.joining(", "))));
    }
}
