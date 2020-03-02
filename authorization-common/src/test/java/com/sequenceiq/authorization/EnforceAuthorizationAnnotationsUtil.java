package com.sequenceiq.authorization;

import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Path;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.springframework.stereotype.Controller;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.AuthorizationResource;

public class EnforceAuthorizationAnnotationsUtil {

    private EnforceAuthorizationAnnotationsUtil() {

    }

    public static void testIfControllerClassHasProperAnnotation() {
        Reflections reflections = new Reflections("com.sequenceiq",
                new FieldAnnotationsScanner(),
                new TypeAnnotationsScanner(),
                new SubTypesScanner(false),
                new MemberUsageScanner());

        Set<Class<?>> apiClasses = reflections.getTypesAnnotatedWith(Path.class);
        Set<String> controllersClasses = Sets.newHashSet();
        apiClasses.stream().forEach(apiClass -> controllersClasses.addAll(
                reflections.getSubTypesOf((Class<Object>) apiClass).stream().map(Class::getSimpleName).collect(Collectors.toSet())));
        Set<String> classesWithControllerAnnotation = reflections.getTypesAnnotatedWith(Controller.class)
                .stream().map(Class::getSimpleName).collect(Collectors.toSet());
        Set<String> controllersWithoutAnnotation = Sets.difference(controllersClasses, classesWithControllerAnnotation);

        assertTrue("These classes are missing @Controller annotation: " + Joiner.on(",").join(controllersWithoutAnnotation),
                controllersWithoutAnnotation.size() == 0);
    }

    public static void testIfControllerClassHasAuthorizationAnnotation() {
        Reflections reflections = new Reflections("com.sequenceiq",
                new FieldAnnotationsScanner(),
                new TypeAnnotationsScanner(),
                new SubTypesScanner(false),
                new MemberUsageScanner());

        Set<String> controllersClasses = reflections.getTypesAnnotatedWith(Controller.class).stream().map(Class::getSimpleName).collect(Collectors.toSet());
        Set<String> authorizationResourceClasses = reflections.getTypesAnnotatedWith(AuthorizationResource.class)
                .stream().map(Class::getSimpleName).collect(Collectors.toSet());
        Set<String> disabledAuthorizationClasses = reflections.getTypesAnnotatedWith(DisableCheckPermissions.class)
                .stream().map(Class::getSimpleName).collect(Collectors.toSet());
        Set<String> controllersWithoutAnnotation = Sets.difference(controllersClasses, Sets.union(authorizationResourceClasses, disabledAuthorizationClasses));

        assertTrue("These controllers are missing @AuthorizationResource annotation: " + Joiner.on(",").join(controllersWithoutAnnotation),
                controllersWithoutAnnotation.size() == 0);
    }
}
