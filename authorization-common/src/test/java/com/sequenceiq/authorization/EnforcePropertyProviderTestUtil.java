package com.sequenceiq.authorization;

import static com.sequenceiq.authorization.EnforceAuthorizationTestUtil.validateMethodByFunction;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN_LIST;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME_LIST;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.authorization.service.AuthorizationEnvironmentCrnListProvider;
import com.sequenceiq.authorization.service.AuthorizationEnvironmentCrnProvider;
import com.sequenceiq.authorization.service.AuthorizationResourceCrnListProvider;
import com.sequenceiq.authorization.service.AuthorizationResourceCrnProvider;
import com.sequenceiq.authorization.service.ResourcePropertyProvider;

public class EnforcePropertyProviderTestUtil {

    private static final Map<Class<? extends Annotation>, Map<Class<? extends ResourcePropertyProvider>, Set<Predicate<Annotation>>>> PROVIDER_VALIDATORS =
            ImmutableMap.<Class<? extends Annotation>, Map<Class<? extends ResourcePropertyProvider>, Set<Predicate<Annotation>>>>builder()
                    .put(CheckPermissionByResourceName.class, Map.of(
                            AuthorizationResourceCrnProvider.class, Set.of(always()),
                            AuthorizationEnvironmentCrnProvider.class, Set.of(hierarchicalAuthNeeded())))
                    .put(CheckPermissionByResourceCrn.class, Map.of(
                            AuthorizationEnvironmentCrnProvider.class, Set.of(hierarchicalAuthNeeded())))
                    .put(CheckPermissionByResourceNameList.class, Map.of(
                            AuthorizationResourceCrnListProvider.class, Set.of(always()),
                            AuthorizationEnvironmentCrnListProvider.class, Set.of(hierarchicalAuthNeeded())))
                    .put(CheckPermissionByResourceCrnList.class, Map.of(
                            AuthorizationEnvironmentCrnListProvider.class, Set.of(hierarchicalAuthNeeded())))
                    .put(CheckPermissionByRequestProperty.class, Map.of(
                            AuthorizationResourceCrnProvider.class, Set.of(variableTypesApplies(NAME)),
                            AuthorizationResourceCrnListProvider.class, Set.of(variableTypesApplies(NAME_LIST)),
                            AuthorizationEnvironmentCrnProvider.class, Set.of(hierarchicalAuthNeeded(), variableTypesApplies(CRN, NAME)),
                            AuthorizationEnvironmentCrnListProvider.class, Set.of(hierarchicalAuthNeeded(), variableTypesApplies(CRN_LIST, NAME_LIST))))
                    .build();

    private static final Map<Class<? extends ResourcePropertyProvider>, Set<Class<? extends ResourcePropertyProvider>>> PROVIDER_SUBTYPES_MAP =
            ImmutableMap.<Class<? extends ResourcePropertyProvider>, Set<Class<? extends ResourcePropertyProvider>>>builder()
                    .put(AuthorizationResourceCrnProvider.class, getSubTypesFiltered(AuthorizationResourceCrnProvider.class))
                    .put(AuthorizationResourceCrnListProvider.class, getSubTypesFiltered(AuthorizationResourceCrnListProvider.class))
                    .put(AuthorizationEnvironmentCrnProvider.class, getSubTypesFiltered(AuthorizationEnvironmentCrnProvider.class))
                    .put(AuthorizationEnvironmentCrnListProvider.class, getSubTypesFiltered(AuthorizationEnvironmentCrnListProvider.class))
                    .build();

    private EnforcePropertyProviderTestUtil() {

    }

    public static void testIfAllNecessaryResourceProviderImplemented() {
        validateMethodByFunction(EnforcePropertyProviderTestUtil::validateMethodForProviders);
    }

    private static List<String> validateMethodForProviders(Method method) {
        List<String> errors = Lists.newArrayList();
        PROVIDER_VALIDATORS.entrySet().forEach(entry -> {
            Class<? extends Annotation> annotationClass = entry.getKey();
            if (method.isAnnotationPresent(annotationClass)) {
                Map<Class<? extends ResourcePropertyProvider>, Set<Predicate<Annotation>>> validatorsByProviderClassMap = entry.getValue();
                validatorsByProviderClassMap.entrySet().stream().forEach(validationEntry ->
                        validateMethodForGivenProvider(method, errors, annotationClass, validationEntry));
            }
        });
        return errors;
    }

    private static void validateMethodForGivenProvider(Method method, List<String> errors, Class<? extends Annotation> annotationClass,
            Map.Entry<Class<? extends ResourcePropertyProvider>, Set<Predicate<Annotation>>> validationEntry) {
        Class<? extends ResourcePropertyProvider> providerClass = validationEntry.getKey();
        Set<Predicate<Annotation>> validators = validationEntry.getValue();
        Annotation annotation = method.getAnnotation(annotationClass);
        Optional<Class<? extends ResourcePropertyProvider>> providerClassPresent =
                validationAnnotationByProvider(providerClass, validators, annotation);
        addErrorIfNeeded(method, errors, providerClass, annotation, providerClassPresent);
    }

    private static void addErrorIfNeeded(Method method, List<String> errors, Class<? extends ResourcePropertyProvider> providerClass,
            Annotation annotation, Optional<Class<? extends ResourcePropertyProvider>> providerClassPresent) {
        if (!providerClassPresent.isPresent()) {
            AuthorizationResourceAction action = getAction(annotation);
            AuthorizationResourceType authorizationResourceType = action.getAuthorizationResourceType();
            errors.add(String.format("Provider with interface %s implemented is needed to authorize using action %s and resource type %s (method: %s)",
                    providerClass.getSimpleName(), action,
                    authorizationResourceType, method.getDeclaringClass().getSimpleName() + "#" + method.getName()));
        }
    }

    private static <T extends ResourcePropertyProvider> Optional<Class<? extends ResourcePropertyProvider>> validationAnnotationByProvider(
            Class<T> propertyProviderClass, Set<Predicate<Annotation>> validationPredicates, Annotation annotation) {
        AuthorizationResourceAction action = getAction(annotation);
        AuthorizationResourceType authorizationResourceType = action.getAuthorizationResourceType();
        if (validationPredicates.stream().allMatch(predicate -> predicate.test(annotation))) {
            return PROVIDER_SUBTYPES_MAP.get(propertyProviderClass).stream()
                    .filter(type -> {
                        ResourcePropertyProvider resourcePropertyProvider = (T) EnforceAuthorizationTestUtil.getSampleObjectFactory().manufacturePojo(type);
                        return authorizationResourceType.equals(resourcePropertyProvider.getSupportedAuthorizationResourceType());
                    })
                    .findFirst();
        }
        return Optional.of(propertyProviderClass);
    }

    private static <T extends ResourcePropertyProvider> Set<Class<? extends ResourcePropertyProvider>> getSubTypesFiltered(Class<T> clazz) {
        return EnforceAuthorizationTestUtil.getReflections().getSubTypesOf(clazz).stream().filter(type -> !type.isInterface()).collect(Collectors.toSet());
    }

    private static Predicate<Annotation> always() {
        return annotation -> true;
    }

    private static Predicate<Annotation> hierarchicalAuthNeeded() {
        return annotation -> getAction(annotation).getAuthorizationResourceType().isHierarchicalAuthorizationNeeded();
    }

    private static Predicate<Annotation> variableTypesApplies(AuthorizationVariableType... variableTypes) {
        return annotation -> {
            if (CheckPermissionByRequestProperty.class.equals(annotation.annotationType())) {
                CheckPermissionByRequestProperty requestPropertyAnnotation = (CheckPermissionByRequestProperty) annotation;
                return Sets.newHashSet(variableTypes).contains(requestPropertyAnnotation.type());
            }
            throw new UnsupportedOperationException("Not valid annotation type");
        };
    }

    private static AuthorizationResourceAction getAction(Annotation annotation) {
        if (CheckPermissionByRequestProperty.class.equals(annotation.annotationType())) {
            return ((CheckPermissionByRequestProperty) annotation).action();
        }
        if (CheckPermissionByResourceCrn.class.equals(annotation.annotationType())) {
            return ((CheckPermissionByResourceCrn) annotation).action();
        }
        if (CheckPermissionByResourceCrnList.class.equals(annotation.annotationType())) {
            return ((CheckPermissionByResourceCrnList) annotation).action();
        }
        if (CheckPermissionByResourceName.class.equals(annotation.annotationType())) {
            return ((CheckPermissionByResourceName) annotation).action();
        }
        if (CheckPermissionByResourceNameList.class.equals(annotation.annotationType())) {
            return ((CheckPermissionByResourceNameList) annotation).action();
        }
        throw new UnsupportedOperationException("Unknown annotation type");
    }
}
