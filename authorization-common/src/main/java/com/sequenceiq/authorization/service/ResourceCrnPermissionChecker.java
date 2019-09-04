package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.authorization.resource.ResourceType;

@Component
public class ResourceCrnPermissionChecker implements PermissionChecker<CheckPermissionByResourceCrn> {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private List<ResourceBasedEnvironmentCrnProvider<? extends Object>> resourceBasedEnvironmentCrnProviders;

    private final Map<Class<? extends Object>, ResourceBasedEnvironmentCrnProvider<? extends Object>> resourceBasedEnvironmentCrnProviderMap = new HashMap<>();

    @PostConstruct
    public void populatePermissionCheckerMap() {
        resourceBasedEnvironmentCrnProviders.forEach(permissionChecker ->
                resourceBasedEnvironmentCrnProviderMap.put(permissionChecker.supportedResourceClass(), permissionChecker));
    }

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, ResourceType resource, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        CheckPermissionByResourceCrn methodAnnotation = (CheckPermissionByResourceCrn) rawMethodAnnotation;
        String resourceCrn = EnvironmentPermissionCheckerUtil.getResourceCrn(proceedingJoinPoint, methodSignature);
        Class relatedObjectClass = methodAnnotation.relatedResourceClass();
        String environmentCrn = resourceBasedEnvironmentCrnProviderMap.get(relatedObjectClass).getEnvironmentCrnByResourceCrn(resourceCrn);
        ResourceAction action = methodAnnotation.action();
        commonPermissionCheckingUtils.checkPermissionForUser(resource, action, userCrn, environmentCrn);
        return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
    }

    @Override
    public Class<CheckPermissionByResourceCrn> supportedAnnotation() {
        return CheckPermissionByResourceCrn.class;
    }
}
