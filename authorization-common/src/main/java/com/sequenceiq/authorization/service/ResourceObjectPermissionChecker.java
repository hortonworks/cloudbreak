package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

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
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

@Component
public class ResourceObjectPermissionChecker implements PermissionChecker<CheckPermissionByResourceObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceObjectPermissionChecker.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Override
    public <T extends Annotation> void checkPermissions(T rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature, long startTime) {
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
                AuthorizationResourceAction action = resourceObjectField.action();
                AuthorizationVariableType authorizationVariableType = resourceObjectField.variableType();
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

    private void checkPermissionForResourceCrnList(String userCrn, Object resultObject, AuthorizationResourceAction action) {
        if (!(resultObject instanceof Collection)) {
            throw new AccessDeniedException("Annotated field within resource object is not collection, thus access is denied!");
        }
        Collection<String> resourceCrns = (Collection<String>) resultObject;
        if (CollectionUtils.isEmpty(resourceCrns)) {
            return;
        }
        commonPermissionCheckingUtils.checkPermissionForUserOnResources(action, userCrn, resourceCrns);
    }

    private void checkPermissionForResourceNameList(String userCrn, Object resultObject, AuthorizationResourceAction action) {
        if (!(resultObject instanceof Collection)) {
            throw new AccessDeniedException("Annotated field within resource object is not collection, thus access is denied!");
        }
        Collection<String> resourceNames = (Collection<String>) resultObject;
        if (CollectionUtils.isEmpty(resourceNames)) {
            return;
        }
        Collection<String> resourceCrns = commonPermissionCheckingUtils.getResourceBasedCrnProvider(action)
                .getResourceCrnListByResourceNameList(Lists.newArrayList(resourceNames));
        commonPermissionCheckingUtils.checkPermissionForUserOnResources(action, userCrn, resourceCrns);
    }

    private void checkPermissionForResourceCrn(String userCrn, Object resultObject, AuthorizationResourceAction action) {
        if (!(resultObject instanceof String)) {
            throw new AccessDeniedException("Annotated field within resource object is not string, thus access is denied!");
        }
        String resourceCrn = (String) resultObject;
        commonPermissionCheckingUtils.checkPermissionForUserOnResource(action, userCrn, resourceCrn);
    }

    private void checkPermissionForResourceName(String userCrn, Object resultObject, AuthorizationResourceAction action) {
        if (!(resultObject instanceof String)) {
            throw new AccessDeniedException("Annotated field within resource object is not string, thus access is denied!");
        }
        String resourceName = (String) resultObject;
        String resourceCrn = commonPermissionCheckingUtils.getResourceBasedCrnProvider(action).getResourceCrnByResourceName(resourceName);
        commonPermissionCheckingUtils.checkPermissionForUserOnResource(action, userCrn, resourceCrn);
    }

    @Override
    public Class<CheckPermissionByResourceObject> supportedAnnotation() {
        return CheckPermissionByResourceObject.class;
    }
}
