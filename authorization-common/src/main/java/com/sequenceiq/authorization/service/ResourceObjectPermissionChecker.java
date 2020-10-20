package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.authorization.annotation.ResourceObjectField;
import com.sequenceiq.authorization.annotation.ResourceObjectFieldHolder;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

@Component
public class ResourceObjectPermissionChecker extends ResourcePermissionChecker<CheckPermissionByResourceObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceObjectPermissionChecker.class);

    @Override
    public <T extends Annotation> void checkPermissions(T rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature, long startTime) {
        // check fields of resourceObject
        CheckPermissionByResourceObject methodAnnotation = (CheckPermissionByResourceObject) rawMethodAnnotation;
        Object resourceObject = getCommonPermissionCheckingUtils().getParameter(proceedingJoinPoint, methodSignature, ResourceObject.class, Object.class);
        checkPermissionOnResourceObjectFields(userCrn, resourceObject, methodAnnotation.deepSearchNeeded());
    }

    private void checkPermissionOnResourceObjectFields(String userCrn, Object resourceObject, boolean deepSearchNeeded) {
        Arrays.stream(FieldUtils.getFieldsWithAnnotation(resourceObject.getClass(), ResourceObjectField.class)).forEach(field -> {
            try {
                field.setAccessible(true);
                Object fieldObject = field.get(resourceObject);
                ResourceObjectField resourceObjectField = field.getAnnotation(ResourceObjectField.class);
                AuthorizationResourceAction action = resourceObjectField.action();
                AuthorizationVariableType authorizationVariableType = resourceObjectField.variableType();
                if (fieldObject != null) {
                    checkObject(userCrn, action, authorizationVariableType, fieldObject);
                } else if (!resourceObjectField.skipAuthzOnNull()) {
                    throw new AccessDeniedException("One of the requestObject's field is null and it should be authorized, " +
                            "thus should be filled in.");
                }
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
        if (deepSearchNeeded) {
            traverseResourceObjectFieldHolders(userCrn, resourceObject, deepSearchNeeded);
        }
    }

    private void traverseResourceObjectFieldHolders(String userCrn, Object resourceObject, boolean deepSearchNeeded) {
        Arrays.stream(FieldUtils.getFieldsWithAnnotation(resourceObject.getClass(), ResourceObjectFieldHolder.class)).forEach(field -> {
            try {
                field.setAccessible(true);
                Object fieldObject = field.get(resourceObject);
                if (fieldObject != null) {
                    if (Collection.class.isAssignableFrom(fieldObject.getClass())) {
                        ((Collection) fieldObject).stream().forEach(collectionObject ->
                                checkPermissionOnResourceObjectFields(userCrn, collectionObject, deepSearchNeeded));
                    } else {
                        checkPermissionOnResourceObjectFields(userCrn, fieldObject, deepSearchNeeded);
                    }
                }
            } catch (Error | RuntimeException unchecked) {
                LOGGER.error("Error happened while traversing the resource object: ", unchecked);
                throw unchecked;
            } catch (Throwable t) {
                LOGGER.error("Error happened while traversing the resource object: ", t);
                throw new AccessDeniedException("Error happened during permission check of resource object, thus access is denied!", t);
            }
        });
    }

    private void checkObject(String userCrn, AuthorizationResourceAction action, AuthorizationVariableType authorizationVariableType, Object resultObject) {
        switch (authorizationVariableType) {
            case NAME:
                checkPermissionForResourceName(userCrn, resultObject, action);
                break;
            case CRN:
                checkPermissionForResourceCrn(userCrn, resultObject, action);
                break;
            case NAME_LIST:
                checkPermissionForResourceNameList(userCrn, resultObject, action);
                break;
            case CRN_LIST:
                checkPermissionForResourceCrnList(userCrn, resultObject, action);
                break;
            default:
                throw new AccessDeniedException("We cannot determine the type of field from authorization point of view, " +
                        "thus access is denied!");
        }
    }

    private void checkPermissionForResourceCrnList(String userCrn, Object resultObject, AuthorizationResourceAction action) {
        if (!(resultObject instanceof Collection)) {
            throw new AccessDeniedException("Annotated field within resource object is not collection, thus access is denied!");
        }
        Collection<String> resourceCrns = (Collection<String>) resultObject;
        if (CollectionUtils.isEmpty(resourceCrns)) {
            return;
        }
        getCommonPermissionCheckingUtils().checkPermissionForUserOnResources(action, userCrn, resourceCrns);
    }

    private void checkPermissionForResourceNameList(String userCrn, Object resultObject, AuthorizationResourceAction action) {
        if (!(resultObject instanceof Collection)) {
            throw new AccessDeniedException("Annotated field within resource object is not collection, thus access is denied!");
        }
        Collection<String> resourceNames = (Collection<String>) resultObject;
        if (CollectionUtils.isEmpty(resourceNames)) {
            return;
        }
        Collection<String> resourceCrns = getCommonPermissionCheckingUtils().getResourceBasedCrnProvider(action)
                .getResourceCrnListByResourceNameList(Lists.newArrayList(resourceNames));
        getCommonPermissionCheckingUtils().checkPermissionForUserOnResources(action, userCrn, resourceCrns);
    }

    private void checkPermissionForResourceCrn(String userCrn, Object resultObject, AuthorizationResourceAction action) {
        if (!(resultObject instanceof String)) {
            throw new AccessDeniedException("Annotated field within resource object is not string, thus access is denied!");
        }
        String resourceCrn = (String) resultObject;
        if (getCommonPermissionCheckingUtils().legacyAuthorizationNeeded()) {
            getCommonPermissionCheckingUtils().checkPermissionForUserOnResource(action, userCrn, resourceCrn);
        } else {
            Map<String, AuthorizationResourceAction> authorizationActions = getAuthorizationActions(resourceCrn, action);
            getCommonPermissionCheckingUtils().checkPermissionForUserOnResource(authorizationActions, userCrn);
        }
    }

    private void checkPermissionForResourceName(String userCrn, Object resultObject, AuthorizationResourceAction action) {
        if (!(resultObject instanceof String)) {
            throw new AccessDeniedException("Annotated field within resource object is not string, thus access is denied!");
        }
        String resourceName = (String) resultObject;
        String resourceCrn = getCommonPermissionCheckingUtils().getResourceBasedCrnProvider(action).getResourceCrnByResourceName(resourceName);
        if (StringUtils.isEmpty(resourceCrn)) {
            throw new NotFoundException(String.format("Could not find resourceCrn for resource by name: %s", resourceName));
        }
        if (getCommonPermissionCheckingUtils().legacyAuthorizationNeeded()) {
            getCommonPermissionCheckingUtils().checkPermissionForUserOnResource(action, userCrn, resourceCrn);
        } else {
            Map<String, AuthorizationResourceAction> authorizationActions = getAuthorizationActions(resourceCrn, action);
            getCommonPermissionCheckingUtils().checkPermissionForUserOnResource(authorizationActions, userCrn);
        }
    }

    @Override
    public Class<CheckPermissionByResourceObject> supportedAnnotation() {
        return CheckPermissionByResourceObject.class;
    }
}
