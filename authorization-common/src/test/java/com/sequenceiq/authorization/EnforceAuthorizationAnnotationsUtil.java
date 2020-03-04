package com.sequenceiq.authorization;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Path;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.MethodParameterNamesScanner;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.TypeElementsScanner;
import org.springframework.stereotype.Controller;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.authorization.resource.AuthorizationApiRequest;

public class EnforceAuthorizationAnnotationsUtil {

    private static final Reflections REFLECTIONS = new Reflections("com.sequenceiq",
            new FieldAnnotationsScanner(),
            new TypeAnnotationsScanner(),
            new SubTypesScanner(false),
            new MemberUsageScanner(),
            new TypeElementsScanner(),
            new MethodAnnotationsScanner(),
            new MethodParameterNamesScanner(),
            new MethodParameterScanner(),
            new ResourcesScanner());

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

    public static void testIfRequestObjectHasProperAuthorizationInterfaceImplemented() {
        Set<Method> controllerMethods = Sets.newHashSet();
        REFLECTIONS.getTypesAnnotatedWith(Controller.class).stream()
                .forEach(controllerClass -> controllerMethods.addAll(Sets.newHashSet(controllerClass.getMethods())));
        Set<Method> methodsAnnotatedWithResourceObjectPermissionCheck = REFLECTIONS.getMethodsAnnotatedWith(CheckPermissionByResourceObject.class);
        Set<Method> relatedMethods = Sets.intersection(controllerMethods, methodsAnnotatedWithResourceObjectPermissionCheck);
        Map<Method, Set<Parameter>> relatedParameters = Maps.newHashMap();
        relatedMethods.stream().forEach(method -> {
            Set<Parameter> parameters = Sets.newHashSet(method.getParameters());
            relatedParameters.put(method, parameters.stream().filter(parameter -> parameter.isAnnotationPresent(ResourceObject.class))
                    .collect(Collectors.toSet()));
        });
        Map<Method, Set<String>> resourceObjectWithoutProperAuthorization = relatedParameters.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> getResourceObjectParameterWithoutProperAuthorization(entry.getValue())));
        Set<String> failedParameterNames = Sets.newHashSet();
        resourceObjectWithoutProperAuthorization.entrySet().stream().forEach(entry -> entry.getValue().stream()
                .forEach(parameterName -> failedParameterNames.add(parameterName +
                        "(related method is " + entry.getKey().getDeclaringClass().getSimpleName() + "#" + entry.getKey().getName() + ")")));

        assertTrue("These parameters do not implement AuthorizationApiRequest interface: " + Joiner.on(",").join(failedParameterNames),
                failedParameterNames.size() == 0);
    }

    private static Set<String> getResourceObjectParameterWithoutProperAuthorization(Set<Parameter> parameters) {
        return parameters.stream().filter(relatedParameter ->
                !Lists.newArrayList(relatedParameter.getType().getInterfaces()).contains(AuthorizationApiRequest.class))
                .map(relatedParameter -> relatedParameter.getType().getSimpleName()).collect(Collectors.toSet());
    }
}
