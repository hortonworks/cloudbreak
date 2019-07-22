package com.sequenceiq.cloudbreak.workspace.authorization;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.service.PermissionChecker;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByWorkspaceCrn;
import com.sequenceiq.authorization.resource.ResourceAction;

@Component
public class WorkspaceCrnPermissionChecker implements PermissionChecker<CheckPermissionsByWorkspaceCrn> {

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, AuthorizationResource resource, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        String workspaceCrn = getWorkspaceCrn(rawMethodAnnotation, proceedingJoinPoint);
        ResourceAction action = getAction(rawMethodAnnotation);
        return permissionCheckingUtils.checkPermissionsByWorkspaceCrnForUserAndProceed(resource, userCrn,
                workspaceCrn, action, proceedingJoinPoint, methodSignature);
    }

    private <T extends Annotation> String getWorkspaceCrn(T rawMethodAnnotation, ProceedingJoinPoint proceedingJoinPoint) {
        CheckPermissionsByWorkspaceCrn methodAnnotation = (CheckPermissionsByWorkspaceCrn) rawMethodAnnotation;
        int workspaceCrnIndex = methodAnnotation.workspaceCrnIndex();
        int length = proceedingJoinPoint.getArgs().length;
        validateWorkspaceCrnIndex(proceedingJoinPoint, workspaceCrnIndex, length);
        return (String) proceedingJoinPoint.getArgs()[workspaceCrnIndex];
    }

    private void validateWorkspaceCrnIndex(JoinPoint proceedingJoinPoint, int workspaceCrnIndex, int length) {
        permissionCheckingUtils.validateIndex(workspaceCrnIndex, length, "workspaceCrnIndex");
        Object workspaceCrn = proceedingJoinPoint.getArgs()[workspaceCrnIndex];
        if (!(workspaceCrn instanceof String)) {
            throw new IllegalArgumentException("Type of workspaceCrn should be String!");
        }
    }

    private <T extends Annotation> ResourceAction getAction(T rawMethodAnnotation) {
        return ((CheckPermissionsByWorkspaceCrn) rawMethodAnnotation).action();
    }

    @Override
    public Class<CheckPermissionsByWorkspaceCrn> supportedAnnotation() {
        return CheckPermissionsByWorkspaceCrn.class;
    }
}
