package com.sequenceiq.redbeams.authorization;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Component
public class DisabledPermissionChecker implements PermissionChecker<DisableCheckPermissions> {

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) throws Throwable {
        return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
    }

    @Override
    public Class<DisableCheckPermissions> supportedAnnotation() {
        return DisableCheckPermissions.class;
    }
}
