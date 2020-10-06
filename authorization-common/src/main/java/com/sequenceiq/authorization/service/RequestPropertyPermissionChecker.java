package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

@Component
public class RequestPropertyPermissionChecker extends ResourcePermissionChecker<CheckPermissionByRequestProperty> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestPropertyPermissionChecker.class);

    @Override
    public <T extends Annotation> void checkPermissions(T rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature, long startTime) {
        // check fields of requestObject
        CheckPermissionByRequestProperty methodAnnotation = (CheckPermissionByRequestProperty) rawMethodAnnotation;
        Object requestObject = getCommonPermissionCheckingUtils().getParameter(proceedingJoinPoint, methodSignature, RequestObject.class, Object.class);
        checkPermissionOnRequestObjectFields(userCrn, requestObject, methodAnnotation);
    }

    private void checkPermissionOnRequestObjectFields(String userCrn, Object resourceObject, CheckPermissionByRequestProperty methodAnnotation) {
        Boolean skipOnNull = methodAnnotation.skipOnNull();
        try {
            Object fieldObject = PropertyUtils.getProperty(resourceObject, methodAnnotation.path());
            AuthorizationVariableType authorizationVariableType = methodAnnotation.type();
            AuthorizationResourceAction action = methodAnnotation.action();
            if (fieldObject != null) {
                checkObject(userCrn, action, authorizationVariableType, fieldObject);
            } else if (!methodAnnotation.skipOnNull()) {
                throw new AccessDeniedException("One of the requestObject's field is null and it should be authorized, " +
                        "thus should be filled in.");
            }
        } catch (NestedNullException nne) {
            if (!skipOnNull) {
                throw new AccessDeniedException("One of the requestObject's field is null and it should be authorized, " +
                        "thus should be filled in.");
            }
        } catch (NotFoundException nfe) {
            LOGGER.warn("Resource not found during permission check of resource object, this should be handled by microservice.");
        } catch (Error | RuntimeException unchecked) {
            LOGGER.error("Error happened during authorization of the request object: ", unchecked);
            throw unchecked;
        } catch (Throwable t) {
            LOGGER.error("Error happened during authorization of the request object: ", t);
            throw new AccessDeniedException("Error happened during authorization of the request object, thus access is denied!", t);
        }
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
            throw new AccessDeniedException("Referred property within request object is not collection, thus access is denied!");
        }
        Collection<String> resourceCrns = (Collection<String>) resultObject;
        if (CollectionUtils.isEmpty(resourceCrns)) {
            return;
        }
        getCommonPermissionCheckingUtils().checkPermissionForUserOnResources(action, userCrn, resourceCrns);
    }

    private void checkPermissionForResourceNameList(String userCrn, Object resultObject, AuthorizationResourceAction action) {
        if (!(resultObject instanceof Collection)) {
            throw new AccessDeniedException("Referred property within request object is not collection, thus access is denied!");
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
            throw new AccessDeniedException("Referred property within request object is not string, thus access is denied!");
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
            throw new AccessDeniedException("Referred property within request object is not string, thus access is denied!");
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
    public Class<CheckPermissionByRequestProperty> supportedAnnotation() {
        return CheckPermissionByRequestProperty.class;
    }
}
