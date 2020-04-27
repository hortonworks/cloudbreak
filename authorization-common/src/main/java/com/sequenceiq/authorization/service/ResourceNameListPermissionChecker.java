package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
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
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

@Component
public class ResourceNameListPermissionChecker implements PermissionChecker<CheckPermissionByResourceNameList> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceNameListPermissionChecker.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private List<ResourceBasedCrnProvider> resourceBasedCrnProviders;

    private final Map<AuthorizationResourceType, ResourceBasedCrnProvider> resourceBasedCrnProviderMap = new HashMap<>();

    @PostConstruct
    public void populateResourceBasedCrnProviderMap() {
        resourceBasedCrnProviders.forEach(resourceBasedCrnProvider ->
                resourceBasedCrnProviderMap.put(resourceBasedCrnProvider.getResourceType(), resourceBasedCrnProvider));
    }

    @Override
    public <T extends Annotation> void checkPermissions(T rawMethodAnnotation, AuthorizationResourceType resourceType, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, long startTime) {
        CheckPermissionByResourceNameList methodAnnotation = (CheckPermissionByResourceNameList) rawMethodAnnotation;
        Collection<String> resourceNames = commonPermissionCheckingUtils
                .getParameter(proceedingJoinPoint, methodSignature, ResourceNameList.class, Collection.class);
        List<String> resourceCrnList = resourceBasedCrnProviderMap.get(resourceType).getResourceCrnListByResourceNameList(Lists.newArrayList(resourceNames));
        AuthorizationResourceAction action = methodAnnotation.action();
        checkActionType(resourceType, action);
        commonPermissionCheckingUtils.checkPermissionForUserOnResources(resourceType, action, userCrn, resourceCrnList);
    }

    @Override
    public Class<CheckPermissionByResourceNameList> supportedAnnotation() {
        return CheckPermissionByResourceNameList.class;
    }

    @Override
    public AuthorizationResourceAction.ActionType actionType() {
        return AuthorizationResourceAction.ActionType.RESOURCE_DEPENDENT;
    }
}
