package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.authorization.service.model.HasRightOnAll;

@Component
public class ResourceCrnListAuthorizationFactory extends TypedAuthorizationFactory<CheckPermissionByResourceCrnList> {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private DefaultResourceAuthorizationProvider defaultResourceAuthorizationProvider;

    @Inject
    private EnvironmentBasedAuthorizationProvider environmentBasedAuthorizationProvider;

    @Override
    public Optional<AuthorizationRule> doGetAuthorization(CheckPermissionByResourceCrnList methodAnnotation, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        AuthorizationResourceAction action = methodAnnotation.action();
        Collection<String> resourceCrns = commonPermissionCheckingUtils
                .getParameter(proceedingJoinPoint, methodSignature, ResourceCrnList.class, Collection.class);
        return calcAuthorization(resourceCrns, action);
    }

    public Optional<AuthorizationRule> calcAuthorization(Collection<String> resourceCrns, AuthorizationResourceAction action) {
        if (CollectionUtils.isEmpty(resourceCrns)) {
            return Optional.empty();
        }
        return defaultResourceAuthorizationProvider.authorizeDefaultOrElseCompute(resourceCrns, action,
                filteredResourceCrns -> {
                    if (commonPermissionCheckingUtils.legacyAuthorizationNeeded()) {
                        return Optional.of(new HasRightOnAll(action, filteredResourceCrns));
                    } else {
                        return environmentBasedAuthorizationProvider.getAuthorizations(filteredResourceCrns, action);
                    }
                });
    }

    @Override
    public Class<CheckPermissionByResourceCrnList> supportedAnnotation() {
        return CheckPermissionByResourceCrnList.class;
    }
}
