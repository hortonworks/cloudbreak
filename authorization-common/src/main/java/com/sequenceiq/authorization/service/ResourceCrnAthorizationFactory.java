package com.sequenceiq.authorization.service;

import java.util.Optional;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.authorization.utils.CrnAccountValidator;

@Component
public class ResourceCrnAthorizationFactory extends TypedAuthorizationFactory<CheckPermissionByResourceCrn> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCrnAthorizationFactory.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private EnvironmentBasedAuthorizationProvider environmentBasedAuthorizationProvider;

    @Inject
    private DefaultResourceAuthorizationProvider defaultResourceAuthorizationProvider;

    @Inject
    private CrnAccountValidator crnAccountValidator;

    @Override
    public Optional<AuthorizationRule> doGetAuthorization(CheckPermissionByResourceCrn methodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature) {
        String resourceCrn = commonPermissionCheckingUtils.getParameter(proceedingJoinPoint, methodSignature, ResourceCrn.class, String.class);
        crnAccountValidator.validateSameAccount(userCrn, resourceCrn);
        AuthorizationResourceAction action = methodAnnotation.action();
        LOGGER.debug("Getting authorization rule to authorize user [{}] for action [{}] over resource [{}]", userCrn, action, resourceCrn);
        return calcAuthorization(resourceCrn, action);
    }

    public Optional<AuthorizationRule> calcAuthorization(String resourceCrn, AuthorizationResourceAction action) {
        return defaultResourceAuthorizationProvider.authorizeDefaultOrElseCompute(resourceCrn, action, () ->
                environmentBasedAuthorizationProvider.getAuthorizations(resourceCrn, action));
    }

    @Override
    public Class<CheckPermissionByResourceCrn> supportedAnnotation() {
        return CheckPermissionByResourceCrn.class;
    }
}
