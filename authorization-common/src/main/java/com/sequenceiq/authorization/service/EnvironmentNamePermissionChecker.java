package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByEnvironmentName;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.authorization.resource.ResourceType;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;

@Component
public class EnvironmentNamePermissionChecker implements PermissionChecker<CheckPermissionByEnvironmentName> {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, ResourceType resource, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        CheckPermissionByEnvironmentName methodAnnotation = (CheckPermissionByEnvironmentName) rawMethodAnnotation;
        String environmentName = EnvironmentPermissionCheckerUtil.getEnvironmentName(proceedingJoinPoint, methodSignature);
        String environmentCrn = environmentEndpoint.getByName(environmentName).getCrn();
        ResourceAction action = methodAnnotation.action();
        commonPermissionCheckingUtils.checkPermissionForUser(resource, action, userCrn, environmentCrn);
        return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
    }

    @Override
    public Class<CheckPermissionByEnvironmentName> supportedAnnotation() {
        return CheckPermissionByEnvironmentName.class;
    }
}
