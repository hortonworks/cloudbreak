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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.authorization.resource.AuthorizableFieldInfoModel;
import com.sequenceiq.authorization.resource.AuthorizationApiRequest;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;

@Component
public class ResourceObjectPermissionChecker implements PermissionChecker<CheckPermissionByResourceObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceObjectPermissionChecker.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private List<ResourceBasedCrnProvider> resourceBasedCrnProviders;

    private final Map<AuthorizationResourceType, ResourceBasedCrnProvider> resourceBasedCrnProviderMap = new HashMap<>();

    @PostConstruct
    public void populateResourceBasedCrnProviderMapMap() {
        resourceBasedCrnProviders.forEach(resourceBasedCrnProvider ->
                resourceBasedCrnProviderMap.put(resourceBasedCrnProvider.getResourceType(), resourceBasedCrnProvider));
    }

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, AuthorizationResourceType resourceType, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, long startTime) {
        // first check the API related resource and method related action
        CheckPermissionByResourceObject methodAnnotation = (CheckPermissionByResourceObject) rawMethodAnnotation;
        AuthorizationResourceAction action = methodAnnotation.action();
        commonPermissionCheckingUtils.checkPermissionForUser(resourceType, action, userCrn);
        // then check resourceObject
        AuthorizationApiRequest resourceObject = commonPermissionCheckingUtils.getParameter(proceedingJoinPoint, methodSignature,
                ResourceObject.class, AuthorizationApiRequest.class);
        checkPermissionOnResourceObjectFields(userCrn, resourceObject);
        return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime);
    }

    private void checkPermissionOnResourceObjectFields(String userCrn, AuthorizationApiRequest resourceObject) {
        try {
            resourceObject.getAuthorizableFields().entrySet().stream().forEach(entry -> {
                String resourceNameOrCrn = entry.getKey();
                AuthorizableFieldInfoModel infoModel = entry.getValue();
                AuthorizationResourceType resourceType = infoModel.getResourceType();
                String resourceCrn = infoModel.getFieldVariableType().equals(AuthorizationVariableType.NAME)
                        ? resourceBasedCrnProviderMap.get(resourceType).getResourceCrnByResourceName(resourceNameOrCrn)
                        : resourceNameOrCrn;
                AuthorizationResourceAction action = infoModel.getAction();
                commonPermissionCheckingUtils.checkPermissionForUserOnResource(resourceType, action, userCrn, resourceCrn);
            });
        } catch (AccessDeniedException e) {
            LOGGER.error("Error happened while traversing the resource object: ", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error happened while traversing the resource object: ", e);
            throw new AccessDeniedException("Error happened during permission check of resource object, thus access is denied!", e);
        }
    }

    @Override
    public Class<CheckPermissionByResourceObject> supportedAnnotation() {
        return CheckPermissionByResourceObject.class;
    }
}
