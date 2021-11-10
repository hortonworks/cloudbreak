package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.authorization.utils.CrnAccountValidator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

@Component
public class RequestPropertyAuthorizationFactory extends TypedAuthorizationFactory<CheckPermissionByRequestProperty> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestPropertyAuthorizationFactory.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private ResourceCrnAthorizationFactory resourceCrnAthorizationProvider;

    @Inject
    private ResourceNameAuthorizationFactory resourceNameAuthorizationProvider;

    @Inject
    private ResourceCrnListAuthorizationFactory resourceCrnListAuthorizationFactory;

    @Inject
    private ResourceNameListAuthorizationFactory resourceNameListAuthorizationFactory;

    @Inject
    private CrnAccountValidator crnAccountValidator;

    @Override
    public Optional<AuthorizationRule> doGetAuthorization(CheckPermissionByRequestProperty methodAnnotation, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        Object requestObject = commonPermissionCheckingUtils.getParameter(proceedingJoinPoint, methodSignature, RequestObject.class, Object.class);
        LOGGER.debug("Getting authorization rule to authorize user [{}] for action [{}] over property [{}] of request object.",
                userCrn, methodAnnotation.action(), methodAnnotation.path());
        return calcAuthorization(requestObject, methodAnnotation, userCrn);
    }

    private Optional<AuthorizationRule> calcAuthorization(Object resourceObject, CheckPermissionByRequestProperty methodAnnotation, String userCrn) {
        boolean skipOnNull = methodAnnotation.skipOnNull();
        try {
            Object fieldObject = PropertyUtils.getProperty(resourceObject, methodAnnotation.path());
            AuthorizationVariableType authorizationVariableType = methodAnnotation.type();
            AuthorizationResourceAction action = methodAnnotation.action();
            if (fieldObject != null) {
                return calcAuthorizationFromObject(action, authorizationVariableType, fieldObject, userCrn);
            } else if (!methodAnnotation.skipOnNull()) {
                throw new BadRequestException(String.format("Property [%s] of the request object must not be null.", methodAnnotation.path()));
            }
        } catch (NestedNullException nne) {
            if (!skipOnNull) {
                throw new BadRequestException(String.format("Property [%s] of the request object must not be null.", methodAnnotation.path()));
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
        return Optional.empty();
    }

    private Optional<AuthorizationRule> calcAuthorizationFromObject(AuthorizationResourceAction action, AuthorizationVariableType authorizationVariableType,
            Object requestObject, String userCrn) {
        switch (authorizationVariableType) {
            case NAME:
                return getAuthorizationFromResourceName(requestObject, action);
            case CRN:
                return getAuthorizationFromResourceCrn(requestObject, action, userCrn);
            case NAME_LIST:
                return getAuthorizationFromResouurceNameList(requestObject, action);
            case CRN_LIST:
                return getAuthorizationFromResourceCrnList(requestObject, action, userCrn);
            default:
                throw new AccessDeniedException("We cannot determine the type of field from authorization point of view, " +
                        "thus access is denied!");
        }
    }

    private Optional<AuthorizationRule> getAuthorizationFromResourceName(Object resultObject, AuthorizationResourceAction action) {
        if (!(resultObject instanceof String)) {
            throw new AccessDeniedException("Referred property within request object is not string, thus access is denied!");
        }
        String resourceName = (String) resultObject;
        return resourceNameAuthorizationProvider.calcAuthorization(resourceName, action);
    }

    private Optional<AuthorizationRule> getAuthorizationFromResourceCrn(Object resultObject, AuthorizationResourceAction action, String userCrn) {
        if (!(resultObject instanceof String)) {
            throw new AccessDeniedException("Referred property within request object is not string, thus access is denied!");
        }
        String resourceCrn = (String) resultObject;
        crnAccountValidator.validateSameAccount(userCrn, resourceCrn);
        return resourceCrnAthorizationProvider.calcAuthorization(resourceCrn, action);
    }

    private Optional<AuthorizationRule> getAuthorizationFromResourceCrnList(Object resultObject, AuthorizationResourceAction action, String userCrn) {
        if (!(resultObject instanceof Collection)) {
            throw new AccessDeniedException("Referred property within request object is not collection, thus access is denied!");
        }
        Collection<String> resourceCrns = (Collection<String>) resultObject;
        crnAccountValidator.validateSameAccount(userCrn, resourceCrns);
        return resourceCrnListAuthorizationFactory.calcAuthorization(resourceCrns, action);
    }

    private Optional<AuthorizationRule> getAuthorizationFromResouurceNameList(Object resultObject, AuthorizationResourceAction action) {
        if (!(resultObject instanceof Collection)) {
            throw new AccessDeniedException("Referred property within request object is not collection, thus access is denied!");
        }
        Collection<String> resourceNames = (Collection<String>) resultObject;
        return resourceNameListAuthorizationFactory.calcAuthorization(resourceNames, action);
    }

    @Override
    public Class<CheckPermissionByRequestProperty> supportedAnnotation() {
        return CheckPermissionByRequestProperty.class;
    }
}
