package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

@Component
public class ResourceNameListPermissionChecker implements PermissionChecker<CheckPermissionByResourceNameList> {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private ResourceBasedEnvironmentCrnProvider resourceBasedEnvironmentCrnProvider;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, AuthorizationResourceType resourceType, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        CheckPermissionByResourceNameList methodAnnotation = (CheckPermissionByResourceNameList) rawMethodAnnotation;
        List<String> resourceNameList = EnvironmentPermissionCheckerUtil.getResourceNameList(proceedingJoinPoint, methodSignature);
        if (!resourceNameList.isEmpty()) {
            List<String> environmentCrnList = resourceBasedEnvironmentCrnProvider.getEnvironmentCrnListByResourceNameList(resourceNameList);
            AuthorizationResourceAction action = methodAnnotation.action();
            environmentCrnList.stream().forEach(environmentCrn ->
                    commonPermissionCheckingUtils.checkPermissionForUser(resourceType, action, userCrn, environmentCrn));
        }
        return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
    }

    @Override
    public Class<CheckPermissionByResourceNameList> supportedAnnotation() {
        return CheckPermissionByResourceNameList.class;
    }
}
