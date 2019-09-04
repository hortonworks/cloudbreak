package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

@Component
public class ResourceNamePermissionChecker implements PermissionChecker<CheckPermissionByResourceName> {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private ResourceBasedEnvironmentCrnProvider resourceBasedEnvironmentCrnProvider;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, AuthorizationResourceType resourceType, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        CheckPermissionByResourceName methodAnnotation = (CheckPermissionByResourceName) rawMethodAnnotation;
        String resourceName = EnvironmentPermissionCheckerUtil.getResourceName(proceedingJoinPoint, methodSignature);
        String environmentCrn = resourceBasedEnvironmentCrnProvider.getEnvironmentCrnByResourceName(resourceName);
        AuthorizationResourceAction action = methodAnnotation.action();
        commonPermissionCheckingUtils.checkPermissionForUser(resourceType, action, userCrn, environmentCrn);
        return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
    }

    @Override
    public Class<CheckPermissionByResourceName> supportedAnnotation() {
        return CheckPermissionByResourceName.class;
    }
}
