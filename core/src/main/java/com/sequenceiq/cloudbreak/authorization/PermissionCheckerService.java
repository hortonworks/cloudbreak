package com.sequenceiq.cloudbreak.authorization;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganization;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganizationId;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByTargetId;
import com.sequenceiq.cloudbreak.aspect.organization.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.user.NullIdentityUserException;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Service
public class PermissionCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCheckerService.class);

    private static final List<Class<? extends Annotation>> POSSIBLE_METHOD_ANNOTATIONS = List.of(CheckPermissionsByOrganization.class,
            CheckPermissionsByOrganizationId.class, CheckPermissionsByTarget.class, CheckPermissionsByTargetId.class,
            CheckPermissionsByReturnValue.class, DisableCheckPermissions.class);

    @Inject
    private UserService userService;

    @Inject
    private List<PermissionChecker<? extends Annotation>> permissionCheckers;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    private final Map<Class<? extends Annotation>, PermissionChecker<? extends Annotation>> permissionCheckerMap = new HashMap<>();

    @PostConstruct
    public void populatePermissionCheckerMap() {
        permissionCheckers.forEach(permissionChecker -> permissionCheckerMap.put(permissionChecker.supportedAnnotation(), permissionChecker));
    }

    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
        }

        Optional<Class<?>> repositoryClass = permissionCheckingUtils.getOrgAwareRepositoryClass(proceedingJoinPoint);
        if (!repositoryClass.isPresent()) {
            return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
        }

        return checkPermission(proceedingJoinPoint, methodSignature, repositoryClass.get());
    }

    private Object checkPermission(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, Class<?> repositoryClass) {
        Optional<Annotation> classAnnotation = permissionCheckingUtils.getClassAnnotation(repositoryClass);
        if (!classAnnotation.isPresent()) {
            throw getAccessDeniedAndLogMissingAnnotation(repositoryClass);
        }

        OrganizationResourceType classOrgResourceType = (OrganizationResourceType) classAnnotation.get();
        if (classOrgResourceType.resource() == OrganizationResource.ALL) {
            throw getAccessDeniedAndLogMissingAnnotation(repositoryClass);
        }

        List<? extends Annotation> annotations = POSSIBLE_METHOD_ANNOTATIONS.stream()
                .map(c -> methodSignature.getMethod().getAnnotation(c))
                .collect(Collectors.toList());

        Annotation methodAnnotation = validateNumberOfAnnotations(methodSignature, annotations);
        if (methodAnnotation instanceof DisableCheckPermissions) {
            return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
        }

        try {
            User user = userService.getCurrentUser();
            PermissionChecker<? extends Annotation> permissionChecker = permissionCheckerMap.get(methodAnnotation.annotationType());
            OrganizationResource resource = classOrgResourceType.resource();
            return permissionChecker.checkPermissions(methodAnnotation, resource, user, proceedingJoinPoint, methodSignature);
        } catch (NullIdentityUserException e) {
            throw new AccessDeniedException("You have no access to this resource.", e);
        }
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
                OrganizationResourceType.class.getName());
        return new AccessDeniedException("You have no access to this resource.");
    }
}
