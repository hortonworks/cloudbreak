package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

@Component
public class ResourceCrnListPermissionChecker implements PermissionChecker<CheckPermissionByResourceCrnList> {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private ResourceBasedEnvironmentCrnProvider resourceBasedEnvironmentCrnProvider;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, AuthorizationResourceType resourceType, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        CheckPermissionByResourceCrnList methodAnnotation = (CheckPermissionByResourceCrnList) rawMethodAnnotation;
        List<String> resourceCrnList = EnvironmentPermissionCheckerUtil.getResourceCrnList(proceedingJoinPoint, methodSignature);
        if (!resourceCrnList.isEmpty()) {
            List<String> environmentCrnList = resourceBasedEnvironmentCrnProvider.getEnvironmentCrnListByResourceCrnList(resourceCrnList);
            AuthorizationResourceAction action = methodAnnotation.action();
            environmentCrnList.stream().forEach(environmentCrn ->
                    commonPermissionCheckingUtils.checkPermissionForUser(resourceType, action, userCrn, environmentCrn));
        }
        return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
    }

    @Override
    public Class<CheckPermissionByResourceCrnList> supportedAnnotation() {
        return CheckPermissionByResourceCrnList.class;
    }
}
