package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Collection;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

@Component
public class ResourceCrnListPermissionChecker implements PermissionChecker<CheckPermissionByResourceCrnList> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCrnListPermissionChecker.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, AuthorizationResourceType resourceType, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, long startTime) {
        CheckPermissionByResourceCrnList methodAnnotation = (CheckPermissionByResourceCrnList) rawMethodAnnotation;
        Collection<String> resourceCrns = commonPermissionCheckingUtils
                .getParameter(proceedingJoinPoint, methodSignature, ResourceCrnList.class, Collection.class);
        AuthorizationResourceAction action = methodAnnotation.action();
        commonPermissionCheckingUtils.checkPermissionForUserOnResources(resourceType, action, userCrn, resourceCrns);
        return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime);
    }

    @Override
    public Class<CheckPermissionByResourceCrnList> supportedAnnotation() {
        return CheckPermissionByResourceCrnList.class;
    }
}
