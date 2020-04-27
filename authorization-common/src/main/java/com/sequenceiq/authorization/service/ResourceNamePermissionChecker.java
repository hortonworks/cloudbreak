package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@Component
public class ResourceNamePermissionChecker implements PermissionChecker<CheckPermissionByResourceName> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceNamePermissionChecker.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Override
    public <T extends Annotation> void checkPermissions(T rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature, long startTime) {
        CheckPermissionByResourceName methodAnnotation = (CheckPermissionByResourceName) rawMethodAnnotation;
        AuthorizationResourceAction action = methodAnnotation.action();
        String resourceName = commonPermissionCheckingUtils.getParameter(proceedingJoinPoint, methodSignature, ResourceName.class, String.class);
        String resourceCrn = commonPermissionCheckingUtils.getResourceBasedCrnProvider(action).getResourceCrnByResourceName(resourceName);
        commonPermissionCheckingUtils.checkPermissionForUserOnResource(action, userCrn, resourceCrn);
    }

    @Override
    public Class<CheckPermissionByResourceName> supportedAnnotation() {
        return CheckPermissionByResourceName.class;
    }
}
