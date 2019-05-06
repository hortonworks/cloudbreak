package com.sequenceiq.cloudbreak.domain;

import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Entity;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Test;
import org.reflections.Reflections;

import com.sequenceiq.cloudbreak.aspect.secret.SecretValue;
import com.sequenceiq.cloudbreak.workspace.model.TenantAwareResource;

public class EntityTest {
    @Test
    public void testIfClassesWithSecretFieldsAreInheritedFromTenantOrWorkspaceAwareResources() {
        Reflections reflections = new Reflections("com.sequenceiq");
        Set<Class<?>> entityClasses = reflections.getTypesAnnotatedWith(Entity.class);
        Set<Class<?>> wrongClasses = entityClasses.stream()
                .filter(cls -> FieldUtils.getFieldsWithAnnotation(cls, SecretValue.class).length > 0)
                .filter(cls -> !TenantAwareResource.class.isAssignableFrom(cls))
                .collect(Collectors.toSet());
        Assert.assertTrue(
                String.format("Classes with Secret fields should be inherited from TenantAwareResource or WorkspaceAwareResoruce. Wrong classes: %s",
                        wrongClasses.stream().map(Class::getName).collect(Collectors.joining(", "))), wrongClasses.isEmpty());
    }
}
