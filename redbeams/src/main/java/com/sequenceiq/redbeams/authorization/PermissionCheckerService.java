package com.sequenceiq.redbeams.authorization;

import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PermissionCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCheckerService.class);

    private static final List<Class<? extends Annotation>> POSSIBLE_METHOD_ANNOTATIONS = List.of(CheckPermissionsByTarget.class,
            CheckPermissionsByReturnValue.class, DisableCheckPermissions.class);

    @Inject
    @VisibleForTesting
    List<PermissionChecker<? extends Annotation>> permissionCheckers;

    @VisibleForTesting
    SecurityContext testSecurityContext;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    private final Map<Class<? extends Annotation>, PermissionChecker<? extends Annotation>> permissionCheckerMap = new HashMap<>();

    @PostConstruct
    public void populatePermissionCheckerMap() {
        permissionCheckers.forEach(permissionChecker -> permissionCheckerMap.put(permissionChecker.supportedAnnotation(), permissionChecker));
    }

    // CHECKSTYLE:OFF
    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
    // CHECKSTYLE:ON
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Authentication auth;
        if (testSecurityContext != null) {
            auth = testSecurityContext.getAuthentication();
        } else {
            auth = SecurityContextHolder.getContext().getAuthentication();
        }
        if (auth == null) {
            return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
        }

        // Class<?> repositoryClass = proceedingJoinPoint.getTarget().getClass();

        List<? extends Annotation> annotations = POSSIBLE_METHOD_ANNOTATIONS.stream()
                .map(c -> methodSignature.getMethod().getAnnotation(c))
                .collect(Collectors.toList());

        Annotation methodAnnotation = validateNumberOfAnnotations(methodSignature, annotations).orElse(null);
        if (methodAnnotation == null) {
            LOGGER.debug("Permission check not present on {}", methodSignature.getName());
            return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
        }
        if (methodAnnotation instanceof DisableCheckPermissions) {
            LOGGER.debug("Permission check disabled on {}", methodSignature.getName());
            return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
        }

        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        if (userCrn == null) {
            throw new AccessDeniedException("User CRN is not available, cannot authorize access");
        }
        PermissionChecker<? extends Annotation> permissionChecker = permissionCheckerMap.get(methodAnnotation.annotationType());
        return permissionChecker.checkPermissions(methodAnnotation, userCrn, proceedingJoinPoint, methodSignature);
    }

    private Optional<Annotation> validateNumberOfAnnotations(MethodSignature methodSignature, List<? extends Annotation> annotations) {
        annotations = annotations.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (annotations.isEmpty()) {
            // OK to not be annotated
            return Optional.empty();
        } else if (annotations.size() > 1) {
            String annotationsMessage = annotations.stream()
                    .map(a -> a.getClass().getSimpleName())
                    .collect(Collectors.joining(",\n"));
            throw new IllegalStateException(format("Only one of these annotations can be added to method: %s", annotationsMessage));
        }
        return Optional.of(annotations.iterator().next());
    }

}
