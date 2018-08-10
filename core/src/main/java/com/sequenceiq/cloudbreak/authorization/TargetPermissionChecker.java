package com.sequenceiq.cloudbreak.authorization;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.domain.organization.User;

@Component
public class TargetPermissionChecker implements PermissionChecker<CheckPermissionsByTarget> {

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, OrganizationResource resource, User user,
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
