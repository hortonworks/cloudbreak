package com.sequenceiq.cloudbreak.authorization;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.domain.workspace.User;

@Component
public class WorkspaceIdPermissionChecker implements PermissionChecker<CheckPermissionsByWorkspaceId> {

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
        CheckPermissionsByWorkspaceId methodAnnotation = (CheckPermissionsByWorkspaceId) rawMethodAnnotation;
        int workspaceIdIndex = methodAnnotation.workspaceIdIndex();
        int length = proceedingJoinPoint.getArgs().length;
        validateWorkspaceIdIndex(proceedingJoinPoint, workspaceIdIndex, length);
        return (Long) proceedingJoinPoint.getArgs()[workspaceIdIndex];
    }

    private void validateWorkspaceIdIndex(JoinPoint proceedingJoinPoint, int workspaceIdIndex, int length) {
        permissionCheckingUtils.validateIndex(workspaceIdIndex, length, "workspaceIdIndex");
        Object workspaceId = proceedingJoinPoint.getArgs()[workspaceIdIndex];
        if (!(workspaceId instanceof Long)) {
            throw new IllegalArgumentException("Type of workspaceId should be Long!");
        }
    }

    private <T extends Annotation> ResourceAction getAction(T rawMethodAnnotation) {
        return ((CheckPermissionsByWorkspaceId) rawMethodAnnotation).action();
    }

    @Override
    public Class<CheckPermissionsByWorkspaceId> supportedAnnotation() {
        return CheckPermissionsByWorkspaceId.class;
    }
}
