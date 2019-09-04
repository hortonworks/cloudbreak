package com.sequenceiq.authorization.service;

import static java.lang.String.format;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

public abstract class AbstractPermissionCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPermissionCheckerService.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private List<PermissionChecker<? extends Annotation>> permissionCheckers;

    private final Map<Class<? extends Annotation>, PermissionChecker<? extends Annotation>> permissionCheckerMap = new HashMap<>();

    @PostConstruct
    public void populatePermissionCheckMap() {
        permissionCheckers.forEach(permissionChecker -> permissionCheckerMap.put(permissionChecker.supportedAnnotation(), permissionChecker));
    }

    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) {
        long startTime = System.currentTimeMillis();
        LOGGER.debug("Permission check started at {}", startTime);
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();

        Optional<Class<?>> authorizationClass = commonPermissionCheckingUtils.getAuthorizationClass(proceedingJoinPoint);
        if (!authorizationClass.isPresent()) {
            throw getAccessDeniedAndLogMissingAnnotation(methodSignature.getMethod().getDeclaringClass());
        }

        return checkPermission(proceedingJoinPoint, methodSignature, authorizationClass.get(), startTime);
    }

    protected abstract List<Class<? extends Annotation>> getPossibleMethodAnnotations();

    protected Object checkPermission(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, Class<?> authorizationClass, long startTime) {
        Optional<Annotation> classAnnotation = commonPermissionCheckingUtils.getClassAnnotation(authorizationClass);

        AuthorizationResource classAuthorizationResource = (AuthorizationResource) classAnnotation.get();

        List<? extends Annotation> annotations = getPossibleMethodAnnotations().stream()
                .map(c -> methodSignature.getMethod().getAnnotation(c))
                .collect(Collectors.toList());

        Annotation methodAnnotation = validateNumberOfAnnotations(methodSignature, annotations);
        if (methodAnnotation instanceof DisableCheckPermissions) {
            return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime);
        }

        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        PermissionChecker<? extends Annotation> permissionChecker = permissionCheckerMap.get(methodAnnotation.annotationType());
        AuthorizationResourceType resource = classAuthorizationResource.type();
        return permissionChecker.checkPermissions(methodAnnotation, resource, userCrn, proceedingJoinPoint, methodSignature, startTime);
    }

    private Annotation validateNumberOfAnnotations(MethodSignature methodSignature, List<? extends Annotation> annotations) {
        annotations = annotations.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (annotations.isEmpty()) {
            throw new IllegalStateException(format("Method must be annotated: %s # %s",
                    methodSignature.getDeclaringTypeName(), methodSignature.getMethod().getName()));
        } else if (annotations.size() > 1) {
            String annotationsMessage = annotations.stream()
                    .map(a -> a.getClass().getSimpleName())
                    .collect(Collectors.joining(",\n"));
            throw new IllegalStateException(format("Only one of these annotations can be added to method: %s", annotationsMessage));
        }
        return annotations.iterator().next();
    }

    private AccessDeniedException getAccessDeniedAndLogMissingAnnotation(Class<?> repositoryClass) {
        LOGGER.error("Class '{}' should be annotated with @{} and specify the resource!", repositoryClass.getCanonicalName(),
                AuthorizationResource.class.getName());
        return new AccessDeniedException("You have no access to this resource.");
    }
}
