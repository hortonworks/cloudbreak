package com.sequenceiq.cloudbreak.authorization;

import static java.lang.String.format;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.user.UserWorkspacePermissionsService;

@Component
public class PermissionCheckingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCheckingUtils.class);

    @Inject
    private UserWorkspacePermissionsService userWorkspacePermissionsService;

    public void checkPermissionByWorkspaceIdForUser(Long workspaceId, WorkspaceResource resource, Action action, User user) {
        UserWorkspacePermissions userWorkspacePermissions = userWorkspacePermissionsService.findForUserByWorkspaceId(workspaceId, user);
        if (userWorkspacePermissions != null) {
            Set<String> permissionSet = userWorkspacePermissions.getPermissionSet();
            boolean hasPermission = WorkspacePermissions.hasPermission(permissionSet, resource, action);
            if (!hasPermission) {
                throw new AccessDeniedException(format("You have no [%s] permission to %s.", action.name(), resource));
            }
        } else {
            throw new AccessDeniedException(format("You have no [%s] permission to %s.", action.name(), resource));
        }
    }

    public void checkPermissionsByTarget(Object target, User user, WorkspaceResource resource, Action action) {
        Iterable<?> iterableTarget = targetToIterable(target);
        Set<Long> workspaceIds = collectWorkspaceIds(iterableTarget);
        if (workspaceIds.isEmpty()) {
            return;
        }
        Set<UserWorkspacePermissions> userWorkspacePermissionSet = userWorkspacePermissionsService.findForUserByWorkspaceIds(user, workspaceIds);
        if (userWorkspacePermissionSet.size() != workspaceIds.size()) {
            throw new AccessDeniedException(format("You have no [%s] permission to %s.", action.name(), resource.getReadableName()));
        }
        userWorkspacePermissionSet.stream()
                .map(UserWorkspacePermissions::getPermissionSet)
                .forEach(permissions -> checkPermissionByPermissionSet(action, resource, permissions));
    }

    private Iterable<?> targetToIterable(Object target) {
        return target instanceof Iterable<?> ? (Iterable<?>) target : Collections.singletonList(target);
    }

    private Set<Long> collectWorkspaceIds(Iterable<?> target) {
        return StreamSupport.stream(target.spliterator(), false)
                .map(resource -> {
                    if (resource instanceof Optional && ((Optional<?>) resource).isPresent()) {
                        return (WorkspaceAwareResource) ((Optional<?>) resource).get();
                    } else if (resource instanceof WorkspaceAwareResource) {
                        return (WorkspaceAwareResource) resource;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .map(this::getWorkspaceId)
                .collect(Collectors.toSet());
    }

    private Long getWorkspaceId(WorkspaceAwareResource workspaceResource) {
        Workspace workspace = workspaceResource.getWorkspace();
        if (workspace == null) {
            throw new IllegalArgumentException("Workspace cannot be null!");
        }
        Long workspaceId = workspace.getId();
        if (workspaceId == null) {
            throw new IllegalArgumentException("WorkspaceId cannot be null!");
        }
        return workspaceId;
    }

    Object checkPermissionsByPermissionSetAndProceed(WorkspaceResource resource, User user, Long workspaceId, Action action,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId cannot be null!");
        }
        UserWorkspacePermissions userWorkspacePermissions = userWorkspacePermissionsService.findForUserByWorkspaceId(user, workspaceId);
        if (userWorkspacePermissions == null) {
            throw new AccessDeniedException(format("You have no [%s] permission to %s.", action.name(), resource.getReadableName()));
        }
        Set<String> permissionSet = userWorkspacePermissions.getPermissionSet();
        LOGGER.info("Checking {} permission for: {}", action, resource);
        checkPermissionByPermissionSet(action, resource, permissionSet);
        return proceed(proceedingJoinPoint, methodSignature);
    }

    void checkPermissionByPermissionSet(Action action, WorkspaceResource resource, Set<String> permissionSet) {
        boolean hasPermission = WorkspacePermissions.hasPermission(permissionSet, resource, action);
        if (!hasPermission) {
            throw new AccessDeniedException(format("You have no [%s] permission to %s.", action.name(), resource.getReadableName()));
        }
    }

    void validateIndex(int index, int length, String indexName) {
        if (index >= length) {
            throw new IllegalArgumentException(
                    format("The %s [%s] cannot be bigger than or equal to the methods argument count [%s]", indexName, index, length));
        }
    }

    public Object proceed(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        try {
            Object proceed = proceedingJoinPoint.proceed();
            if (proceed == null) {
                LOGGER.info("Return value is null, method signature: {}", methodSignature.toLongString());
            }
            return proceed;
        } catch (Throwable t) {
            throw new AccessDeniedException(t.getMessage(), t);
        }
    }

    Optional<Class<?>> getWorkspaceAwareRepositoryClass(ProceedingJoinPoint proceedingJoinPoint) {
        return Arrays.stream(proceedingJoinPoint.getTarget().getClass().getInterfaces())
                .filter(i -> Arrays.asList(i.getInterfaces()).contains(WorkspaceResourceRepository.class))
                .findFirst();
    }

    Optional<Annotation> getClassAnnotation(Class<?> repositoryClass) {
        return Arrays.stream(repositoryClass.getAnnotations())
                .filter(a -> a.annotationType().equals(WorkspaceResourceType.class))
                .findFirst();
    }
}
