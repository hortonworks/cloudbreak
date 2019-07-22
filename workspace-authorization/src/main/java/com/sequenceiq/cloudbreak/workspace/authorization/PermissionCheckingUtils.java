package com.sequenceiq.cloudbreak.workspace.authorization;

import static java.lang.String.format;

import java.util.Collections;
import java.util.List;
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

import com.google.common.base.Joiner;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@Component
public class PermissionCheckingUtils extends CommonPermissionCheckingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCheckingUtils.class);

    @Inject
    private UmsWorkspaceAuthorizationService umsWorkspaceAuthorizationService;

    public void checkPermissionsByTarget(Object target, String userCrn, AuthorizationResource resource, ResourceAction action) {
        Iterable<?> iterableTarget = targetToIterable(target);
        Set<Long> workspaceIds = collectWorkspaceIds(iterableTarget);
        if (workspaceIds.isEmpty()) {
            return;
        }
        List<Long> idsWithFailedCheck = workspaceIds.stream()
                .filter(workspaceId -> !umsWorkspaceAuthorizationService.hasRightOfUserForResource(userCrn, resource, action))
                .collect(Collectors.toList());
        if (!idsWithFailedCheck.isEmpty()) {
            throw new AccessDeniedException(format("You have no [%s] permission to these workspaces: %s.", action.name(),
                    Joiner.on(",").join(idsWithFailedCheck)));
        }

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

    private Set<String> collectWorkspaceCrns(Iterable<?> target) {
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
                .map(this::getWorkspaceCrn)
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

    private String getWorkspaceCrn(WorkspaceAwareResource workspaceResource) {
        Workspace workspace = workspaceResource.getWorkspace();
        if (workspace == null) {
            throw new IllegalArgumentException("Workspace cannot be null!");
        }
        String workspaceCrn = workspace.getResourceCrn();
        if (workspaceCrn == null) {
            throw new IllegalArgumentException("workspaceCrn cannot be null!");
        }
        return workspaceCrn;
    }

    Object checkPermissionsByWorkspaceIdForUserAndProceed(AuthorizationResource resource, String userCrn, Long workspaceId, ResourceAction action,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId cannot be null!");
        }
        checkPermissionForUser(resource, action, userCrn);
        return proceed(proceedingJoinPoint, methodSignature);
    }

    Object checkPermissionsByWorkspaceCrnForUserAndProceed(AuthorizationResource resource, String userCrn, String workspaceCrn, ResourceAction action,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        if (workspaceCrn == null) {
            throw new IllegalArgumentException("workspaceCrn cannot be null!");
        }
        checkPermissionForUser(resource, action, userCrn);
        return proceed(proceedingJoinPoint, methodSignature);
    }

    void validateIndex(int index, int length, String indexName) {
        if (index >= length) {
            throw new IllegalArgumentException(
                    format("The %s [%s] cannot be bigger than or equal to the methods argument count [%s]", indexName, index, length));
        }
    }
}
