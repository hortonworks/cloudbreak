package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@Component
public class ResourceCrnPermissionChecker extends ResourcePermissionChecker<CheckPermissionByResourceCrn> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCrnPermissionChecker.class);

    @Override
    public <T extends Annotation> void checkPermissions(T rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature, long startTime) {
        CheckPermissionByResourceCrn methodAnnotation = (CheckPermissionByResourceCrn) rawMethodAnnotation;
        String resourceCrn = getCommonPermissionCheckingUtils().getParameter(proceedingJoinPoint, methodSignature, ResourceCrn.class, String.class);
        AuthorizationResourceAction action = methodAnnotation.action();
        if (getCommonPermissionCheckingUtils().legacyAuthorizationNeeded()) {
            getCommonPermissionCheckingUtils().checkPermissionForUserOnResource(action, userCrn, resourceCrn);
        } else {
            Map<String, AuthorizationResourceAction> authorizationActions = getAuthorizationActions(resourceCrn, action);
            getCommonPermissionCheckingUtils().checkPermissionForUserOnResource(authorizationActions, userCrn);
        }
    }

    @Override
    public Class<CheckPermissionByResourceCrn> supportedAnnotation() {
        return CheckPermissionByResourceCrn.class;
    }
}
