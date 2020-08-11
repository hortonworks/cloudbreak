package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

public abstract class ResourcePermissionChecker<T extends Annotation> implements PermissionChecker<T> {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    protected Map<String, AuthorizationResourceAction> getAuthorizationActions(String resourceCrn, AuthorizationResourceAction action) {
        ResourceBasedCrnProvider resourceBasedCrnProvider = commonPermissionCheckingUtils.getResourceBasedCrnProvider(action);
        if (resourceBasedCrnProvider == null) {
            return Collections.emptyMap();
        }
        Map<String, AuthorizationResourceAction> authorizationActions = new HashMap<>();
        authorizationActions.put(resourceCrn, action);
        resourceBasedCrnProvider.getEnvironmentCrnByResourceCrn(resourceCrn).ifPresent(environmentCrn -> authorizationActions.put(environmentCrn, action));
        return authorizationActions;
    }

    public CommonPermissionCheckingUtils getCommonPermissionCheckingUtils() {
        return commonPermissionCheckingUtils;
    }

}
