package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermission;
import com.sequenceiq.authorization.resource.ResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;

@Component
public class DefaultPermissionChecker implements PermissionChecker<CheckPermission> {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, ResourceType resource, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        CheckPermission methodAnnotation = (CheckPermission) rawMethodAnnotation;
        ResourceAction action = methodAnnotation.action();
        commonPermissionCheckingUtils.checkPermissionForUser(resource, action, userCrn);
        return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
    }

    @Override
    public Class<CheckPermission> supportedAnnotation() {
        return CheckPermission.class;
    }
}
