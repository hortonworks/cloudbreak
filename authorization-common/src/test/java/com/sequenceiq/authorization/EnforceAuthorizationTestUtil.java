package com.sequenceiq.authorization;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.MethodParameterNamesScanner;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.springframework.stereotype.Controller;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;

import uk.co.jemos.podam.api.PodamFactoryImpl;

public class EnforceAuthorizationTestUtil {

    private static final Reflections REFLECTIONS = new Reflections("com.sequenceiq",
        new FieldAnnotationsScanner(),
        new TypeAnnotationsScanner(),
        new SubTypesScanner(false),
        new MemberUsageScanner(),
        new MethodAnnotationsScanner(),
        new MethodParameterScanner(),
        new MethodParameterNamesScanner());

    private static final PodamFactoryImpl SAMPLE_OBJECT_FACTORY = new PodamFactoryImpl();

    private EnforceAuthorizationTestUtil() {

    }

    public static Reflections getReflections() {
        return REFLECTIONS;
    }

    public static PodamFactoryImpl getSampleObjectFactory() {
        return SAMPLE_OBJECT_FACTORY;
    }

    public static void validateMethodByFunction(Function<Method, List<String>> validatorFunction) {
        Set<Class<?>> authorizationResourceClasses = REFLECTIONS.getTypesAnnotatedWith(Controller.class);
        Set<Class<?>> disabledAuthzOrInternalOnlyClasses = Sets.union(REFLECTIONS.getTypesAnnotatedWith(InternalOnly.class),
            REFLECTIONS.getTypesAnnotatedWith(DisableCheckPermissions.class));
        List<String> validationErrors = Sets.difference(authorizationResourceClasses,
                        Sets.union(Set.of("ExampleAuthorizationResourceClass"), disabledAuthzOrInternalOnlyClasses))
                .stream()
                .map(Class::getDeclaredMethods)
                .flatMap(Arrays::stream)
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(validatorFunction)
                .flatMap(Collection::stream)
                .collect(toList());
        assertTrue(Joiner.on(System.lineSeparator()).join(validationErrors), validationErrors.isEmpty());
    }
}
