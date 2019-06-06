package com.sequenceiq.redbeams.authorization;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Component
public class ReturnValuePermissionChecker implements PermissionChecker<CheckPermissionsByReturnValue> {

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) throws Throwable {
        CheckPermissionsByReturnValue methodAnnotation = (CheckPermissionsByReturnValue) rawMethodAnnotation;
        ResourceAction action = methodAnnotation.action();
        Object result = permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
        permissionCheckingUtils.checkPermissionsByTarget(result, userCrn, action);
        return result;
    }

    @Override
    public Class<CheckPermissionsByReturnValue> supportedAnnotation() {
        return CheckPermissionsByReturnValue.class;
    }
}
