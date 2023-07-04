package com.sequenceiq.cloudbreak.core.flow2;

import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.rotation.flow.config.SecretRotationFlowConfig;
import com.sequenceiq.cloudbreak.rotation.flow.status.SecretRotationStatusChangeFlowConfig;
import com.sequenceiq.flow.core.config.FlowConfiguration;

public class EnforceFlowFinalizer {

    private static final Reflections REFLECTIONS = new Reflections("com.sequenceiq.cloudbreak",
            new SubTypesScanner(false));

    private static final Set<Class> EXCEPTIONS = Set.of(SecretRotationFlowConfig.class, SecretRotationStatusChangeFlowConfig.class);

    @Test
    public void enforceStackStatusFlowFinalizer() {
        Set<Class<? extends FlowConfiguration>> flowConfigs = REFLECTIONS.getSubTypesOf(FlowConfiguration.class);
        Set<String> flowConfigsWithoutFinalizer = flowConfigs.stream()
                .filter(clazz -> !EXCEPTIONS.contains(clazz))
                .filter(not(Class::isInterface))
                .filter(not(Class::isAnonymousClass))
                .filter(not(Class::isLocalClass))
                .filter(not(Class::isMemberClass))
                .filter(flowConfig -> !Modifier.isAbstract(flowConfig.getModifiers()))
                .filter(flowConfig -> !StackStatusFinalizerAbstractFlowConfig.class.isAssignableFrom(flowConfig))
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        assertTrue(flowConfigsWithoutFinalizer.isEmpty(), String.format("These classes should inherit from %s%n %s",
                StackStatusFinalizerAbstractFlowConfig.class.getSimpleName(), Joiner.on("\n").join(flowConfigsWithoutFinalizer)));
    }
}
