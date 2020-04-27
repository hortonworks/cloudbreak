package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@Component
public class ResourceNameListPermissionChecker implements PermissionChecker<CheckPermissionByResourceNameList> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceNameListPermissionChecker.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Override
    public <T extends Annotation> void checkPermissions(T rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature, long startTime) {
        CheckPermissionByResourceNameList methodAnnotation = (CheckPermissionByResourceNameList) rawMethodAnnotation;
        AuthorizationResourceAction action = methodAnnotation.action();
        Collection<String> resourceNames = commonPermissionCheckingUtils
                .getParameter(proceedingJoinPoint, methodSignature, ResourceNameList.class, Collection.class);
        List<String> resourceCrnList = commonPermissionCheckingUtils.getResourceBasedCrnProvider(action)
                .getResourceCrnListByResourceNameList(Lists.newArrayList(resourceNames));
        commonPermissionCheckingUtils.checkPermissionForUserOnResources(action, userCrn, resourceCrnList);
    }

    @Override
    public Class<CheckPermissionByResourceNameList> supportedAnnotation() {
        return CheckPermissionByResourceNameList.class;
    }
}
