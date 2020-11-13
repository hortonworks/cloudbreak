package com.sequenceiq.authorization.service;

import java.util.Optional;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.authorization.service.model.HasRight;

@Component
public class ResourceCrnAthorizationFactory extends TypedAuthorizationFactory<CheckPermissionByResourceCrn> {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private EnvironmentBasedAuthorizationProvider environmentBasedAuthorizationProvider;

    @Inject
    private DefaultResourceAuthorizationProvider defaultResourceAuthorizationProvider;

    @Override
    public Optional<AuthorizationRule> doGetAuthorization(CheckPermissionByResourceCrn methodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature) {
        String resourceCrn = commonPermissionCheckingUtils.getParameter(proceedingJoinPoint, methodSignature, ResourceCrn.class, String.class);
        AuthorizationResourceAction action = methodAnnotation.action();
        return calcAuthorization(resourceCrn, action);
    }

    public Optional<AuthorizationRule> calcAuthorization(String resourceCrn, AuthorizationResourceAction action) {
        return defaultResourceAuthorizationProvider.authorizeDefaultOrElseCompute(resourceCrn, action, () -> {
            if (commonPermissionCheckingUtils.legacyAuthorizationNeeded()) {
                return Optional.of(new HasRight(action, resourceCrn));
            } else {
                return environmentBasedAuthorizationProvider.getAuthorizations(resourceCrn, action);
            }
        });
    }

    @Override
    public Class<CheckPermissionByResourceCrn> supportedAnnotation() {
        return CheckPermissionByResourceCrn.class;
    }
}
