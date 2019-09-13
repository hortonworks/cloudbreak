package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByEnvironmentCrn;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.authorization.resource.ResourceType;

@Component
public class EnvironmentCrnPermissionChecker implements PermissionChecker<CheckPermissionByEnvironmentCrn> {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, ResourceType resource, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        CheckPermissionByEnvironmentCrn methodAnnotation = (CheckPermissionByEnvironmentCrn) rawMethodAnnotation;
        String environmentCrn = EnvironmentPermissionCheckerUtil.getEnvironmentCrn(proceedingJoinPoint, methodSignature);
        ResourceAction action = methodAnnotation.action();
        commonPermissionCheckingUtils.checkPermissionForUser(resource, action, userCrn, environmentCrn);
        return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
    }

    @Override
    public Class<CheckPermissionByEnvironmentCrn> supportedAnnotation() {
        return CheckPermissionByEnvironmentCrn.class;
    }
}
