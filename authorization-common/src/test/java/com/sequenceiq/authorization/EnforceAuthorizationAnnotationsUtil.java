package com.sequenceiq.authorization;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
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
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.util.AuthorizationAnnotationUtils;

public class EnforceAuthorizationAnnotationsUtil {

    private static final Reflections REFLECTIONS = new Reflections("com.sequenceiq",
            new FieldAnnotationsScanner(),
            new TypeAnnotationsScanner(),
            new SubTypesScanner(false),
            new MemberUsageScanner());

    private EnforceAuthorizationAnnotationsUtil() {

    }

    public static void testIfControllerClassHasProperAnnotation() {
        Set<Class<?>> apiClasses = REFLECTIONS.getTypesAnnotatedWith(Path.class);
        Set<String> controllersClasses = Sets.newHashSet();
        apiClasses.stream().forEach(apiClass -> controllersClasses.addAll(
                REFLECTIONS.getSubTypesOf((Class<Object>) apiClass).stream().map(Class::getSimpleName).collect(Collectors.toSet())));
        Set<String> classesWithControllerAnnotation = REFLECTIONS.getTypesAnnotatedWith(Controller.class)
                .stream().map(Class::getSimpleName).collect(Collectors.toSet());
        Set<String> controllersWithoutAnnotation = Sets.difference(controllersClasses, classesWithControllerAnnotation);

        assertTrue("These classes are missing @Controller annotation: " + Joiner.on(",").join(controllersWithoutAnnotation),
                controllersWithoutAnnotation.size() == 0);
    }

    public static void testIfControllerClassHasAuthorizationAnnotation() {
        Set<String> controllersClasses = REFLECTIONS.getTypesAnnotatedWith(Controller.class).stream().map(Class::getSimpleName).collect(Collectors.toSet());
        Set<String> authorizationResourceClasses = REFLECTIONS.getTypesAnnotatedWith(AuthorizationResource.class)
                .stream().map(Class::getSimpleName).collect(Collectors.toSet());
        Set<String> disabledAuthorizationClasses = REFLECTIONS.getTypesAnnotatedWith(DisableCheckPermissions.class)
                .stream().map(Class::getSimpleName).collect(Collectors.toSet());
        Set<String> controllersWithoutAnnotation = Sets.difference(controllersClasses, Sets.union(authorizationResourceClasses, disabledAuthorizationClasses));

        assertTrue("These controllers are missing @AuthorizationResource annotation: " + Joiner.on(",").join(controllersWithoutAnnotation),
                controllersWithoutAnnotation.size() == 0);
    }

    public static void testIfControllerMethodsHaveProperAuthorizationAnnotation() {
        Set<Class<?>> authorizationResourceClasses = REFLECTIONS.getTypesAnnotatedWith(AuthorizationResource.class);
        Set<String> methodsWithoutAnnotation = Sets.newHashSet();
        authorizationResourceClasses.stream().forEach(authzClass -> Arrays.stream(authzClass.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()) && !AuthorizationAnnotationUtils.getPossibleMethodAnnotations().stream()
                    .filter(annotation -> method.isAnnotationPresent(annotation)).findAny().isPresent())
            .forEach(method -> methodsWithoutAnnotation.add(authzClass.getSimpleName() + "#" + method.getName())));

        assertTrue("These controller methods are missing any authorization related annotation: "
                        + Joiner.on(",").join(methodsWithoutAnnotation), methodsWithoutAnnotation.size() == 0);
    }
}
