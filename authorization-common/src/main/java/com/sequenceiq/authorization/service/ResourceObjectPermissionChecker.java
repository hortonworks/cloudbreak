package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.authorization.annotation.ResourceObjectField;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

@Component
public class ResourceObjectPermissionChecker implements PermissionChecker<CheckPermissionByResourceObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceObjectPermissionChecker.class);

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
        // check fields of resourceObject
        Object resourceObject = commonPermissionCheckingUtils.getParameter(proceedingJoinPoint, methodSignature, ResourceObject.class, Object.class);
        checkPermissionOnResourceObjectFields(userCrn, resourceObject);
    }

    private void checkPermissionOnResourceObjectFields(String userCrn, Object resourceObject) {
        Arrays.stream(FieldUtils.getFieldsWithAnnotation(resourceObject.getClass(), ResourceObjectField.class)).forEach(field -> {
            try {
                field.setAccessible(true);
                ResourceObjectField resourceObjectField = field.getAnnotation(ResourceObjectField.class);
                Object resultObject = field.get(resourceObject);
                if (!(resultObject instanceof String)) {
                    throw new AccessDeniedException("Annotated field within resource object is not string, thus access is denied!");
                }
                String resourceNameOrCrn = (String) resultObject;
                String resourceCrn = resourceObjectField.variableType().equals(AuthorizationVariableType.NAME)
                        ? resourceBasedCrnProviderMap.get(resourceObjectField.type()).getResourceCrnByResourceName(resourceNameOrCrn)
                        : resourceNameOrCrn;
                AuthorizationResourceAction action = resourceObjectField.action();
                checkActionType(resourceObjectField.type(), action);
                commonPermissionCheckingUtils.checkPermissionForUserOnResource(resourceObjectField.type(), action, userCrn, resourceCrn);
            } catch (NotFoundException nfe) {
                LOGGER.warn("Resource not found during permission check of resource object, this should be handled by microservice.");
            } catch (Error | RuntimeException unchecked) {
                LOGGER.error("Error happened while traversing the resource object: ", unchecked);
                throw unchecked;
            } catch (Throwable t) {
                LOGGER.error("Error happened while traversing the resource object: ", t);
                throw new AccessDeniedException("Error happened during permission check of resource object, thus access is denied!", t);
            }
        });
    }

    @Override
    public Class<CheckPermissionByResourceObject> supportedAnnotation() {
        return CheckPermissionByResourceObject.class;
    }

    @Override
    public AuthorizationResourceAction.ActionType actionType() {
        return AuthorizationResourceAction.ActionType.RESOURCE_DEPENDENT;
    }
}
