package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
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

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

@Component
public class ResourceNamePermissionChecker implements PermissionChecker<CheckPermissionByResourceName> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceNamePermissionChecker.class);

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
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, AuthorizationResourceType resourceType, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, long startTime) {
        CheckPermissionByResourceName methodAnnotation = (CheckPermissionByResourceName) rawMethodAnnotation;
        String resourceName = commonPermissionCheckingUtils.getParameter(proceedingJoinPoint, methodSignature, ResourceName.class, String.class);
        String resourceCrn = resourceBasedCrnProviderMap.get(resourceType).getResourceCrnByResourceName(resourceName);
        AuthorizationResourceAction action = methodAnnotation.action();
        checkActionType(resourceType, action);
        commonPermissionCheckingUtils.checkPermissionForUserOnResource(resourceType, action, userCrn, resourceCrn);
        return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime);
    }

    @Override
    public Class<CheckPermissionByResourceName> supportedAnnotation() {
        return CheckPermissionByResourceName.class;
    }

    @Override
    public AuthorizationResourceAction.ActionType actionType() {
        return AuthorizationResourceAction.ActionType.RESOURCE_DEPENDENT;
    }
}
