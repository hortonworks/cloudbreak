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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByTargetId;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspace;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.aspect.workspace.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Service
public class PermissionCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCheckerService.class);

    private static final List<Class<? extends Annotation>> POSSIBLE_METHOD_ANNOTATIONS = List.of(CheckPermissionsByWorkspace.class,
            CheckPermissionsByWorkspaceId.class, CheckPermissionsByTarget.class, CheckPermissionsByTargetId.class,
            CheckPermissionsByReturnValue.class, DisableCheckPermissions.class);

    @Inject
    private UserService userService;

    @Inject
    private List<PermissionChecker<? extends Annotation>> permissionCheckers;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    private final Map<Class<? extends Annotation>, PermissionChecker<? extends Annotation>> permissionCheckerMap = new HashMap<>();

    @PostConstruct
    public void populatePermissionCheckerMap() {
        permissionCheckers.forEach(permissionChecker -> permissionCheckerMap.put(permissionChecker.supportedAnnotation(), permissionChecker));
    }

    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
        }

        OAuth2Authentication oAuth = auth instanceof OAuth2Authentication ? (OAuth2Authentication) auth : null;
        if (oAuth != null && oAuth.getUserAuthentication() == null && oAuth.getOAuth2Request().getScope().contains(SpecialScopes.AUTO_SCALE.getScope())) {
            return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
        }

        Optional<Class<?>> repositoryClass = permissionCheckingUtils.getWorkspaceAwareRepositoryClass(proceedingJoinPoint);
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

        WorkspaceResourceType classWorkspaceResourceType = (WorkspaceResourceType) classAnnotation.get();
        if (classWorkspaceResourceType.resource() == WorkspaceResource.ALL) {
            throw getAccessDeniedAndLogMissingAnnotation(repositoryClass);
        }

        List<? extends Annotation> annotations = POSSIBLE_METHOD_ANNOTATIONS.stream()
                .map(c -> methodSignature.getMethod().getAnnotation(c))
                .collect(Collectors.toList());

        Annotation methodAnnotation = validateNumberOfAnnotations(methodSignature, annotations);
        if (methodAnnotation instanceof DisableCheckPermissions) {
            return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
        }

        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        PermissionChecker<? extends Annotation> permissionChecker = permissionCheckerMap.get(methodAnnotation.annotationType());
        WorkspaceResource resource = classWorkspaceResourceType.resource();
        return permissionChecker.checkPermissions(methodAnnotation, resource, user, proceedingJoinPoint, methodSignature);
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
                WorkspaceResourceType.class.getName());
        return new AccessDeniedException("You have no access to this resource.");
    }
}
