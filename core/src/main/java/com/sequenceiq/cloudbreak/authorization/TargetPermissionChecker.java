package com.sequenceiq.cloudbreak.authorization;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;

@Component
public class TargetPermissionChecker implements PermissionChecker<CheckPermissionsByTarget> {

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, WorkspaceResource resource, User user,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {

        CheckPermissionsByTarget methodAnnotation = (CheckPermissionsByTarget) rawMethodAnnotation;
        int targetIndex = methodAnnotation.targetIndex();
        int length = proceedingJoinPoint.getArgs().length;
        permissionCheckingUtils.validateIndex(targetIndex, length, "targetIndex");
        Object target = proceedingJoinPoint.getArgs()[targetIndex];
        permissionCheckingUtils.checkPermissionsByTarget(target, user, resource, methodAnnotation.action());
        return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
    }

    @Override
    public Class<CheckPermissionsByTarget> supportedAnnotation() {
        return CheckPermissionsByTarget.class;
    }
}
