package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;

@Component
public class DefaultPermissionChecker implements PermissionChecker<CheckPermission> {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, AuthorizationResource resource, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        CheckPermission methodAnnotation = (CheckPermission) rawMethodAnnotation;
        ResourceAction action = methodAnnotation.action();
        Object proceed = commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
        if (proceed instanceof Optional<?>) {
            Optional<?> optionalResult = (Optional<?>) proceed;
            if (!optionalResult.isPresent()) {
                return proceed;
            }
            commonPermissionCheckingUtils.checkPermissionForUser(resource, action, userCrn);
        } else {
            commonPermissionCheckingUtils.checkPermissionForUser(resource, action, userCrn);
        }
        return proceed;
    }

    @Override
    public Class<CheckPermission> supportedAnnotation() {
        return CheckPermission.class;
    }
}
