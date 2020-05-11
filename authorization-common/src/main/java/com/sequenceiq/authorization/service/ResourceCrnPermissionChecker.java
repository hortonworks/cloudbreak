package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@Component
public class ResourceCrnPermissionChecker implements PermissionChecker<CheckPermissionByResourceCrn> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCrnPermissionChecker.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Override
    public <T extends Annotation> void checkPermissions(T rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature, long startTime) {
        CheckPermissionByResourceCrn methodAnnotation = (CheckPermissionByResourceCrn) rawMethodAnnotation;
        String resourceCrn = commonPermissionCheckingUtils.getParameter(proceedingJoinPoint, methodSignature, ResourceCrn.class, String.class);
        AuthorizationResourceAction action = methodAnnotation.action();
        commonPermissionCheckingUtils.checkPermissionForUserOnResource(action, userCrn, resourceCrn);
    }

    @Override
    public Class<CheckPermissionByResourceCrn> supportedAnnotation() {
        return CheckPermissionByResourceCrn.class;
    }
}
