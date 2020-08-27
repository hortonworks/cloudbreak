package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@Component
public class ResourceNamePermissionChecker extends ResourcePermissionChecker<CheckPermissionByResourceName> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceNamePermissionChecker.class);

    @Override
    public <T extends Annotation> void checkPermissions(T rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature, long startTime) {
        CheckPermissionByResourceName methodAnnotation = (CheckPermissionByResourceName) rawMethodAnnotation;
        AuthorizationResourceAction action = methodAnnotation.action();
        String resourceName = getCommonPermissionCheckingUtils().getParameter(proceedingJoinPoint, methodSignature, ResourceName.class, String.class);
        ResourceBasedCrnProvider resourceBasedCrnProvider = getCommonPermissionCheckingUtils().getResourceBasedCrnProvider(action);
        String resourceCrn = resourceBasedCrnProvider.getResourceCrnByResourceName(resourceName);
        Map<String, AuthorizationResourceAction> authorizationActions = getAuthorizationActions(resourceCrn, action);
        getCommonPermissionCheckingUtils().checkPermissionForUserOnResource(authorizationActions, userCrn);
    }

    @Override
    public Class<CheckPermissionByResourceName> supportedAnnotation() {
        return CheckPermissionByResourceName.class;
    }
}
