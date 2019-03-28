package com.sequenceiq.cloudbreak.authorization;

import static java.lang.String.format;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

@Component
public class WorkspacePermissionChecker implements PermissionChecker<CheckPermissionsByWorkspace> {

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, WorkspaceResource resource, User user,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        Long workspaceId = getWorkspaceId(rawMethodAnnotation, proceedingJoinPoint);
        ResourceAction action = getAction(rawMethodAnnotation);
        return permissionCheckingUtils.checkPermissionsByWorkspaceIdForUserAndProceed(resource, user, workspaceId, action, proceedingJoinPoint, methodSignature);
    }

    private <T extends Annotation> Long getWorkspaceId(T rawMethodAnnotation, ProceedingJoinPoint proceedingJoinPoint) {
        CheckPermissionsByWorkspace methodAnnotation = (CheckPermissionsByWorkspace) rawMethodAnnotation;
        int workspaceIndex = methodAnnotation.workspaceIndex();
        int length = proceedingJoinPoint.getArgs().length;
        validateWorkspaceIndex(proceedingJoinPoint, workspaceIndex, length);
        return ((Workspace) proceedingJoinPoint.getArgs()[workspaceIndex]).getId();
    }

    private void validateWorkspaceIndex(JoinPoint proceedingJoinPoint, int workspaceIndex, int length) {
        permissionCheckingUtils.validateIndex(workspaceIndex, length, "workspaceIndex");
        Object workspace = proceedingJoinPoint.getArgs()[workspaceIndex];
        if (!(workspace instanceof Workspace)) {
            throw new IllegalArgumentException(format("Type of workspace should be %s!", Workspace.class.getCanonicalName()));
        }
    }

    private <T extends Annotation> ResourceAction getAction(T rawMethodAnnotation) {
        return ((CheckPermissionsByWorkspace) rawMethodAnnotation).action();
    }

    @Override
    public Class<CheckPermissionsByWorkspace> supportedAnnotation() {
        return CheckPermissionsByWorkspace.class;
    }
}
