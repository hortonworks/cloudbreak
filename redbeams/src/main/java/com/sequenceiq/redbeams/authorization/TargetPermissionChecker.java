package com.sequenceiq.redbeams.authorization;

import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Component
public class TargetPermissionChecker implements PermissionChecker<CheckPermissionsByTarget> {

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) throws Throwable {

        CheckPermissionsByTarget methodAnnotation = (CheckPermissionsByTarget) rawMethodAnnotation;
        int targetIndex = methodAnnotation.targetIndex();
        int length = proceedingJoinPoint.getArgs().length;
        permissionCheckingUtils.validateIndex(targetIndex, length, "targetIndex");
        Object target = proceedingJoinPoint.getArgs()[targetIndex];
        if (target instanceof Optional) {
            Optional<?> targetOpt = (Optional<?>) target;
            if (!targetOpt.isPresent()) {
                return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
            }
            target = targetOpt.get();
        }
        permissionCheckingUtils.checkPermissionsByTarget(target, userCrn, methodAnnotation.action());
        return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
    }

    @Override
    public Class<CheckPermissionsByTarget> supportedAnnotation() {
        return CheckPermissionsByTarget.class;
    }
}
