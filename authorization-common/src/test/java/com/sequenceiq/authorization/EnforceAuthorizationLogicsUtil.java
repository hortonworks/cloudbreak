package com.sequenceiq.authorization;

import static com.sequenceiq.authorization.EnforceAuthorizationLogicsUtil.GenericType.list;
import static com.sequenceiq.authorization.EnforceAuthorizationLogicsUtil.GenericType.set;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.Path;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByCompositeRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.util.AuthorizationAnnotationUtils;

import uk.co.jemos.podam.api.PodamFactoryImpl;

public class EnforceAuthorizationLogicsUtil {

    private static final Reflections REFLECTIONS = new Reflections("com.sequenceiq",
            new FieldAnnotationsScanner(),
            new TypeAnnotationsScanner(),
            new SubTypesScanner(false),
            new MemberUsageScanner(),
            new MethodAnnotationsScanner(),
            new MethodParameterScanner(),
            new MethodParameterNamesScanner());

    private static final PodamFactoryImpl SAMPLE_OBJECT_FACTORY = new PodamFactoryImpl();

    private static final Map<Class<? extends Annotation>, Function<Method, Optional<String>>> METHOD_VALIDATORS =
            ImmutableMap.<Class<? extends Annotation>, Function<Method, Optional<String>>>builder()
                    .put(CheckPermissionByResourceCrn.class, stringParam(ResourceCrn.class))
                    .put(CheckPermissionByResourceName.class, stringParam(ResourceName.class))
                    .put(CheckPermissionByResourceCrnList.class, anyCollectionFrom(ResourceCrnList.class, list(String.class), set(String.class)))
                    .put(CheckPermissionByResourceNameList.class, anyCollectionFrom(ResourceNameList.class, list(String.class), set(String.class)))
                    .put(DisableCheckPermissions.class, noRestriction())
                    .put(CheckPermissionByAccount.class, noRestriction())
                    .put(InternalOnly.class, noRestriction())
                    .put(CustomPermissionCheck.class, noRestriction())
                    .put(FilterListBasedOnPermissions.class, noRestriction())
                    .put(CheckPermissionByRequestProperty.class, hasParamWhere(RequestObject.class, requestObject()))
                    .put(CheckPermissionByCompositeRequestProperty.class, hasParamWhere(RequestObject.class, requestObject()))
                    .build();

    private EnforceAuthorizationLogicsUtil() {

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
                controllersWithoutAnnotation.isEmpty());
    }

    public static void testIfControllerMethodsHaveProperAuthorizationAnnotation(Set<String> exclude) {
        Set<Class<?>> authorizationResourceClasses = REFLECTIONS.getTypesAnnotatedWith(Controller.class);
        Set<Class<?>> disabledAuthzOrInternalOnlyClasses = Sets.union(REFLECTIONS.getTypesAnnotatedWith(InternalOnly.class),
                REFLECTIONS.getTypesAnnotatedWith(DisableCheckPermissions.class));
        List<String> validationErrors = Sets.difference(authorizationResourceClasses, Sets.union(exclude, disabledAuthzOrInternalOnlyClasses))
                .stream()
                .map(Class::getDeclaredMethods)
                .flatMap(Arrays::stream)
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(EnforceAuthorizationLogicsUtil::validateMethod)
                .flatMap(Collection::stream)
                .collect(toList());
        assertTrue(Joiner.on(System.lineSeparator()).join(validationErrors), validationErrors.isEmpty());
    }

    private static List<String> validateMethod(Method method) {
        List<Class<? extends Annotation>> annotations = AuthorizationAnnotationUtils
                .getPossibleMethodAnnotations()
                .stream()
                .filter(method::isAnnotationPresent)
                .collect(toList());
        if (annotations.isEmpty()) {
            return List.of(invalid(method, "Missing authz annotation."));
        } else {
            return annotations
                    .stream()
                    .map(annotation -> METHOD_VALIDATORS.getOrDefault(annotation, unknownAnnotation(annotation)).apply(method))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(Object::toString)
                    .collect(toList());
        }
    }

    private static String invalid(Method method, String reason) {
        return methodToString(method) + ": " + reason;
    }

    private static Function<Method, Optional<String>> noRestriction() {
        return method -> Optional.empty();
    }

    private static Function<Method, Optional<String>> unknownAnnotation(Class<? extends Annotation> annotation) {
        return method -> Optional.of(invalid(method, "No validation rule specified for " + annotation));
    }

    private static String methodToString(Method method) {
        return method.getDeclaringClass().getSimpleName() + '#' + method.getName();
    }

    private static Function<Method, Optional<String>> hasParamWhere(Class<? extends Annotation> annotation,
            Function<Pair<Method, Class<?>>, Optional<String>> typeValidator) {
        return method -> {
            List<Optional<String>> validations = Arrays.stream(method.getParameters())
                    .filter(parameter -> parameter.isAnnotationPresent(annotation))
                    .map(parameter -> typeValidator.apply(Pair.of(method, parameter.getType())))
                    .collect(toList());
            String errorMessageCommon = " method parameter with @" + annotation.getSimpleName() + " annotation";
            if (validations.isEmpty()) {
                return Optional.of(invalid(method, "Misssing" + errorMessageCommon));
            } else if (validations.size() > 1) {
                return Optional.of(invalid(method, "Multiple" + errorMessageCommon));
            } else {
                return validations.get(0);
            }
        };
    }

    private static Function<Method, Optional<String>> stringParam(Class<? extends Annotation> annotation) {
        return hasParam(annotation, String.class);
    }

    private static Function<Method, Optional<String>> hasParam(Class<? extends Annotation> annotation, Class<?> type) {
        return method -> {
            long count = Arrays.stream(method.getParameters())
                    .filter(parameter -> parameter.isAnnotationPresent(annotation) && type.equals(parameter.getType()))
                    .count();
            String errorMessageCommon = " method parameter with @" + annotation.getSimpleName() + " annotation and type " + type.getSimpleName();
            return evaluateResult(method, count, errorMessageCommon);
        };
    }

    private static Function<Method, Optional<String>> anyCollectionFrom(Class<? extends Annotation> annotation, GenericType... genericTypes) {
        return method -> {
            long count = Arrays.stream(method.getParameters())
                    .filter(parameter -> parameter.isAnnotationPresent(annotation))
                    .map(parameter -> Arrays.stream(genericTypes)
                            .filter(genericType -> genericType.wrapperType.isAssignableFrom(parameter.getType()))
                            .filter(genericType -> {
                                Type[] actualTypeArguments = ((ParameterizedType) parameter.getParameterizedType()).getActualTypeArguments();
                                return actualTypeArguments.length == 1 && actualTypeArguments[0].equals(genericType.genericType);
                            })
                            .count())
                    .reduce(0L, Long::sum);
            String errorMessageCommon = " method parameter with @" + annotation.getSimpleName() + " annotation and type from " +
                    Arrays.stream(genericTypes)
                            .map(GenericType::toString)
                            .collect(Collectors.joining(",", "[", "]"));
            return evaluateResult(method, count, errorMessageCommon);
        };
    }

    private static Optional<String> evaluateResult(Method method, long count, String errorMessageCommon) {
        if (count == 0) {
            return Optional.of(invalid(method, "Misssing" + errorMessageCommon));
        } else if (count > 1) {
            return Optional.of(invalid(method, "Multiple" + errorMessageCommon));
        } else {
            return Optional.empty();
        }
    }

    private static Function<Pair<Method, Class<?>>, Optional<String>> requestObject() {
        return ctx -> {
            Method method = ctx.getKey();
            Class<?> type = ctx.getValue();
            try {
                Object requestSample = SAMPLE_OBJECT_FACTORY.manufacturePojo(type);
                Set<String> errorMessages = Sets.newHashSet();
                Arrays.stream(method.getAnnotations())
                        .forEach(annotation -> {
                            if (annotation.annotationType().equals(CheckPermissionByRequestProperty.class)) {
                                checkRequestPropertyAnnotation(method, requestSample, errorMessages, (CheckPermissionByRequestProperty) annotation);
                            } else if (annotation.annotationType().equals(CheckPermissionByCompositeRequestProperty.class)) {
                                Arrays.stream(((CheckPermissionByCompositeRequestProperty) annotation).value()).forEach(byRequestProperty ->
                                        checkRequestPropertyAnnotation(method, requestSample, errorMessages, byRequestProperty));
                            }
                        });
                if (errorMessages.isEmpty()) {
                    return Optional.empty();
                } else {
                    return Optional.of(Joiner.on(",").join(errorMessages));
                }
            } catch (Exception e) {
                return Optional.of(String.format("Error during checking request object of %s#%s: %s",
                        method.getDeclaringClass().getSimpleName(), method.getName(), e.getMessage()));
            }
        };
    }

    private static void checkRequestPropertyAnnotation(Method method, Object sample, Set<String> errorMessages, CheckPermissionByRequestProperty annotation) {
        try {
            Class<?> propertyType = PropertyUtils.getPropertyType(sample, annotation.path());
            switch (annotation.type()) {
                case CRN:
                case NAME:
                    if (!propertyType.equals(String.class)) {
                        errorMessages.add(String.format("%s property of request object in %s#%s should be String",
                                annotation.path(), method.getDeclaringClass().getSimpleName(), method.getName()));
                    }
                    break;
                case NAME_LIST:
                case CRN_LIST:
                    if (!Collection.class.isAssignableFrom(propertyType)) {
                        errorMessages.add(String.format("%s property of request object in %s#%s should be a subType of Collection<String>",
                                annotation.path(), method.getDeclaringClass().getSimpleName(), method.getName()));
                        break;
                    }
                    Collection property = (Collection) PropertyUtils.getProperty(sample, annotation.path());
                    if (!property.iterator().next().getClass().equals(String.class)) {
                        errorMessages.add(String.format("%s collection property of request object in %s#%s should contain Strings",
                                annotation.path(), method.getDeclaringClass().getSimpleName(), method.getName()));
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            errorMessages.add(String.format("Error regarding %s in %s#%s: %s %s", annotation.path(),
                    method.getDeclaringClass().getSimpleName(), method.getName(), e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    public static class GenericType {

        private final Class<?> wrapperType;

        private final Class<?> genericType;

        GenericType(Class<?> wrapperType, Class<?> genericType) {
            this.wrapperType = wrapperType;
            this.genericType = genericType;
        }

        static GenericType list(Class<?> genericType) {
            return new GenericType(List.class, genericType);
        }

        static GenericType set(Class<?> genericType) {
            return new GenericType(Set.class, genericType);
        }

        @Override
        public String toString() {
            return wrapperType.getSimpleName() + '<' + genericType.getSimpleName() + '>';
        }
    }
}
